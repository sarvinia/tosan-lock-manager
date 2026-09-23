package com.tosan.tools.lockmanager.impl.memory;

import com.tosan.tools.lockmanager.exception.LockManagerRunTimeException;
import com.tosan.tools.lockmanager.exception.LockManagerTimeoutException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
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

    private final ConcurrentHashMap<String, ReentrantReadWriteLock> lockRegistry = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ReentrantLock> convertLockRegistry = new ConcurrentHashMap<>();

    public void requestReadLock(String lockNameType, String lockName, Integer lockTimeout, boolean releaseOnCommit) {
        if (releaseOnCommit)
            LOGGER.warn("releaseOnCommit is not supported by MemoryLockService and will be ignored.");
        int timeout = lockTimeout != null ? lockTimeout : DEFAULT_READ_LOCK_TIMEOUT;
        String lockHandle = getLockHandle(lockNameType, lockName);
        LOGGER.debug("Requesting read lock with handle {}", lockHandle);
        ReentrantReadWriteLock lock = getLockInstance(lockHandle);
        try {
            boolean granted = lock.readLock().tryLock(timeout, TimeUnit.SECONDS);
            if (!granted) {
                throw new LockManagerTimeoutException("Timeout error occurred in 'IN_MEMORY_LOCK' request.");
            }
            LOGGER.debug("Acquired read lock with handle {}.", lockHandle);
        } catch (InterruptedException e) {
            throw new LockManagerTimeoutException("Timeout error occurred in 'IN_MEMORY_LOCK' request.");
        }
    }

    public void requestWriteLock(String lockNameType, String lockName, Integer lockTimeout, boolean releaseOnCommit) {
        if (releaseOnCommit)
            LOGGER.warn("releaseOnCommit is not supported by MemoryLockService and will be ignored.");
        int timeout = lockTimeout != null ? lockTimeout : DEFAULT_WRITE_LOCK_TIMEOUT;
        String lockHandle = getLockHandle(lockNameType, lockName);
        LOGGER.debug("Requesting write lock with handle {}", lockHandle);
        ReentrantReadWriteLock lock = getLockInstance(lockHandle);
        try {
            boolean granted = lock.writeLock().tryLock(timeout, TimeUnit.SECONDS);
            if (!granted) {
                throw new LockManagerTimeoutException("Timeout error occurred in 'IN_MEMORY_LOCK' request.");
            }
            LOGGER.debug("Acquired write lock with handle {}.", lockHandle);
        } catch (InterruptedException e) {
            throw new LockManagerTimeoutException("Timeout error occurred in 'IN_MEMORY_LOCK' request.");
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
        ReentrantReadWriteLock lock = lockRegistry.get(lockHandle);
        if (lock == null || !lock.isWriteLockedByCurrentThread()) {
            throw new LockManagerRunTimeException("Thread does not own write lock to convert.");
        }
        requestReadLock(lockNameType, lockName, lockTimeout, false);
        lock.writeLock().unlock();
        LOGGER.debug("Converted to read lock with handle {}", lockHandle);
    }

    /**
     * Upgrades a read lock held by the current thread to a write lock.
     * {@link ReentrantReadWriteLock} does not support a direct read-to-write upgrade (another reader
     * could be waiting), so the read lock is released first and the write lock is then requested.
     */
    public void convertToWriteLock(String lockNameType, String lockName, Integer lockTimeout) {
        String lockHandle = getLockHandle(lockNameType, lockName);
        LOGGER.debug("Requesting convert to write lock with handle {}", lockHandle);
        ReentrantReadWriteLock lock = lockRegistry.get(lockHandle);
        if (lock == null || lock.getReadHoldCount() == 0) {
            throw new LockManagerRunTimeException("Thread does not own any read lock to convert.");
        }
        ReentrantLock convertLock = getConvertLockInstance(lockHandle);
        if (!convertLock.tryLock()) {
            throw new LockManagerTimeoutException("Another thread is converting this lock!");
        }
        try {
            lock.readLock().unlock();
            try {
                requestWriteLock(lockNameType, lockName, lockTimeout, false);
            } catch (LockManagerTimeoutException e) {
                requestReadLock(lockNameType, lockName, lockTimeout, false);
                throw e;
            }
            requestWriteLock(lockNameType, lockName, lockTimeout, false);
            LOGGER.debug("Converted to write lock with handle {}", lockHandle);
        } finally {
            convertLock.unlock();
        }
    }

    public void unLock(String lockNameType, String lockName) {
        String lockHandle = getLockHandle(lockNameType, lockName);
        LOGGER.debug("Requesting release lock with handle {}", lockHandle);
        ReentrantReadWriteLock lock = lockRegistry.get(lockHandle);
        if (lock == null) {
            return;
        }
        try {
            if (lock.isWriteLockedByCurrentThread()) {
                lock.writeLock().unlock();
            }
            if (lock.getReadHoldCount() > 0) {
                lock.readLock().unlock();
            }
            LOGGER.debug("Released lock with handle {}", lockHandle);
        } catch (IllegalMonitorStateException e) {
            LOGGER.debug("Current thread is not owner of lock");
        }
    }

    private ReentrantReadWriteLock getLockInstance(String lockHandle) {
        return lockRegistry.computeIfAbsent(lockHandle, handle -> new ReentrantReadWriteLock());
    }

    private ReentrantLock getConvertLockInstance(String lockHandle) {
        return convertLockRegistry.computeIfAbsent(lockHandle, handle -> new ReentrantLock());
    }

    private String getLockHandle(String lockNameType, String lockName) {
        LOGGER.debug("Requesting lock handle for lock with name '{}'.", lockName);
        return lockNameType + (StringUtils.isNotEmpty(lockName) ? "-" + lockName : "");
    }
}
