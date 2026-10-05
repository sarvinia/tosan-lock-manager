package com.tosan.tools.lockmanager.impl.memory;

import com.tosan.tools.lockmanager.exception.LockManagerRunTimeException;
import com.tosan.tools.lockmanager.exception.LockManagerTimeoutException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * JVM-based lock service with no external client needed.
 * every lock is a {@link ReentrantReadWriteLock} kept in a process-local map,keyed by lock handle.
 * This makes it non-distributed by design. It only coordinates threads inside the current JVM.
 * So it is meant for single-instance deployments only.
 * Because the lock's lifetime is tied to this JVM's heap, if this JVM dies, every lock it held disappears with it.
 */
public class MemoryLockService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MemoryLockService.class);
    private static final int DEFAULT_READ_LOCK_TIMEOUT = 60;
    private static final int DEFAULT_WRITE_LOCK_TIMEOUT = 7200;
    private static final TimeUnit LOCK_TTL_UNIT = TimeUnit.SECONDS;
    private final ConcurrentHashMap<String, LockWrapper> lockRegistry = new ConcurrentHashMap<>();
    private long lockTimeToLive = 600L;

    public void setLockTimeToLive(long lockTimeToLive) {
        this.lockTimeToLive = lockTimeToLive;
    }

    public void requestReadLock(String lockNameType, String lockName, Integer lockTimeout, boolean releaseOnCommit) {
        if (releaseOnCommit)
            LOGGER.warn("releaseOnCommit is not supported by MemoryLockService and will be ignored.");
        int timeout = lockTimeout != null ? lockTimeout : DEFAULT_READ_LOCK_TIMEOUT;
        String lockHandle = getLockHandle(lockNameType, lockName);
        LOGGER.debug("Requesting read lock with handle {}", lockHandle);
        LockWrapper wrapper = acquireWrapper(lockHandle);
        boolean granted = false;
        try {
            granted = wrapper.getLock().readLock().tryLock(timeout, TimeUnit.SECONDS);
            if (!granted) {
                throw new LockManagerTimeoutException("Timeout error occurred in 'IN_MEMORY_LOCK' request.");
            }
            LOGGER.debug("Acquired read lock with handle {}.", lockHandle);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LockManagerTimeoutException("Timeout error occurred in 'IN_MEMORY_LOCK' request.");
        } finally {
            if (!granted) {
                releaseWrapper(lockHandle, wrapper);
            }
        }
    }

    public void requestWriteLock(String lockNameType, String lockName, Integer lockTimeout, boolean releaseOnCommit) {
        if (releaseOnCommit)
            LOGGER.warn("releaseOnCommit is not supported by MemoryLockService and will be ignored.");
        int timeout = lockTimeout != null ? lockTimeout : DEFAULT_WRITE_LOCK_TIMEOUT;
        String lockHandle = getLockHandle(lockNameType, lockName);
        LOGGER.debug("Requesting write lock with handle {}", lockHandle);
        LockWrapper wrapper = acquireWrapper(lockHandle);
        boolean granted = false;
        try {
            granted = wrapper.getLock().writeLock().tryLock(timeout, TimeUnit.SECONDS);
            if (!granted) {
                throw new LockManagerTimeoutException("Timeout error occurred in 'IN_MEMORY_LOCK' request.");
            }
            LOGGER.debug("Acquired write lock with handle {}.", lockHandle);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LockManagerTimeoutException("Timeout error occurred in 'IN_MEMORY_LOCK' request.");
        } finally {
            if (!granted) {
                releaseWrapper(lockHandle, wrapper);
            }
        }
    }

    /**
     * Downgrades a write lock held by the current thread to a read lock.
     * The read lock is acquired before the write lock is released, so the
     * conversion is atomic - nobody can get a write lock in between.
     */
    public void convertToReadLock(String lockNameType, String lockName, Integer lockTimeout) {
        String lockHandle = getLockHandle(lockNameType, lockName);
        LOGGER.debug("Requesting convert to read lock with handle {}", lockHandle);
        LockWrapper wrapper = lockRegistry.get(lockHandle);
        if (wrapper == null || wrapper.isExpired())
            throw new LockManagerRunTimeException("Conversion failed because lock was not found or is expired.");
        ReentrantReadWriteLock lock = wrapper.getLock();
        if (!lock.isWriteLockedByCurrentThread()) {
            throw new LockManagerRunTimeException("Thread does not own write lock to convert.");
        }
        try {
            boolean granted = lock.readLock().tryLock(lockTimeout != null ? lockTimeout : DEFAULT_READ_LOCK_TIMEOUT, TimeUnit.SECONDS);
            if (!granted) {
                throw new LockManagerTimeoutException("Timeout error occurred in 'IN_MEMORY_LOCK' request.");
            }
            lock.writeLock().unlock();
            LOGGER.debug("Converted to read lock with handle {}", lockHandle);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LockManagerTimeoutException("Timeout error occurred in 'IN_MEMORY_LOCK' request.");
        }
    }

    /**
     * Upgrades a read lock held by the current thread to a write lock.
     * {@link ReentrantReadWriteLock} does not support a direct read-to-write upgrade (another reader
     * could be waiting), so the read lock is released first and the write lock is then requested.
     */
    public void convertToWriteLock(String lockNameType, String lockName, Integer lockTimeout) {
        String lockHandle = getLockHandle(lockNameType, lockName);
        LOGGER.debug("Requesting convert to write lock with handle {}", lockHandle);
        LockWrapper wrapper = lockRegistry.get(lockHandle);
        if (wrapper == null || wrapper.isExpired()) {
            throw new LockManagerRunTimeException("Conversion failed because lock was not found or is expired.");
        }
        ReentrantReadWriteLock lock = wrapper.getLock();
        if (lock.getReadHoldCount() == 0) {
            throw new LockManagerRunTimeException("Thread does not own any read lock to convert.");
        }
        if (!wrapper.getConversionLock().tryLock()) {
            throw new LockManagerTimeoutException("Another thread is converting this lock!");
        }
        try {
            int readHolds = lock.getReadHoldCount();
            for (int i = 0; i < readHolds; i++) {
                lock.readLock().unlock();
            }
            try {
                requestWriteLock(lockNameType, lockName, lockTimeout, false);
            } catch (LockManagerTimeoutException e) {
                for (int i = 0; i < readHolds; i++) {
                    requestReadLock(lockNameType, lockName, lockTimeout, false);
                }
                throw e;
            } finally {
                for (int i = 0; i < readHolds; i++) {
                    releaseWrapper(lockHandle, wrapper);
                }
            }
            LOGGER.debug("Converted to write lock with handle {}", lockHandle);
        } finally {
            wrapper.getConversionLock().unlock();
        }
    }

    public void unLock(String lockNameType, String lockName) {
        String lockHandle = getLockHandle(lockNameType, lockName);
        LOGGER.debug("Requesting release lock with handle {}", lockHandle);
        LockWrapper wrapper = lockRegistry.get(lockHandle);
        if (wrapper == null) {
            LOGGER.debug("No lock wrapper found for handle {}", lockHandle);
            return;
        }
        ReentrantReadWriteLock lock = wrapper.getLock();
        try {
            if (lock.isWriteLockedByCurrentThread()) {
                lock.writeLock().unlock();
                releaseWrapper(lockHandle, wrapper);
                LOGGER.debug("Released write lock with handle {}", lockHandle);
                return;
            }
            if (lock.getReadHoldCount() > 0) {
                lock.readLock().unlock();
                releaseWrapper(lockHandle, wrapper);
                LOGGER.debug("Released read lock with handle {}", lockHandle);
                return;
            }
            LOGGER.debug("Current thread is not the owner of lock with handle {}", lockHandle);
        } catch (IllegalMonitorStateException e) {
            LOGGER.debug("Current thread is not the owner of lock with handle {}", lockHandle);
        }
    }

    private LockWrapper acquireWrapper(String lockHandle) {
        return lockRegistry.compute(lockHandle, (handle, existingWrapper) -> {
                    if (existingWrapper == null) {
                        LOGGER.debug("Creating new lock wrapper for handle {}", handle);
                        existingWrapper = new LockWrapper(lockTimeToLive, LOCK_TTL_UNIT);
                    } else if (existingWrapper.isExpired()) {
                        LOGGER.debug("Lock wrapper for handle {} has expired. Creating a new wrapper.", handle);
                        existingWrapper = new LockWrapper(lockTimeToLive, LOCK_TTL_UNIT);
                    }
                    existingWrapper.incrementUsers();
                    return existingWrapper;
                }
        );
    }

    private void releaseWrapper(String lockHandle, LockWrapper wrapper) {
        lockRegistry.computeIfPresent(lockHandle, (handle, current) -> {
            if (current != wrapper) {
                return current;
            }
            if (current.decrementUsersAndIsUnused()) {
                LOGGER.debug("Removing unused lock wrapper for handle {}", handle);
                return null;
            }
            return current;
        });
    }

    private String getLockHandle(String lockNameType, String lockName) {
        LOGGER.debug("Requesting lock handle for lock with name '{}'.", lockName);
        return lockNameType + (StringUtils.isNotEmpty(lockName) ? "-" + lockName : "");
    }
}
