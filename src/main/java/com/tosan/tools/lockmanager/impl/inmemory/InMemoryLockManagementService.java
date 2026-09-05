package com.tosan.tools.lockmanager.impl.inmemory;

import com.tosan.tools.lockmanager.api.LockManagementService;
import com.tosan.tools.lockmanager.exception.LockManagerRunTimeException;
import com.tosan.tools.lockmanager.exception.LockManagerTimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InMemoryLockManagementService implements LockManagementService {

    private static final Logger LOGGER = LoggerFactory.getLogger(InMemoryLockManagementService.class);
    private final InMemoryLockService inMemoryLockService;

    public InMemoryLockManagementService(InMemoryLockService inMemoryLockService) {
        this.inMemoryLockService = inMemoryLockService;
    }

    @Override
    public void requestReadLock(String lockNameType, boolean releaseOnCommit) throws LockManagerTimeoutException {
        try {
            inMemoryLockService.requestReadLock(lockNameType, null, null, releaseOnCommit);
        } catch (LockManagerTimeoutException e) {
            LOGGER.warn("Write lock '{}' held by another thread: " + e.getMessage(), lockNameType);
            throw e;
        }
    }

    @Override
    public void requestReadLock(String lockNameType, String lockName, boolean releaseOnCommit) throws LockManagerTimeoutException {
        try {
            inMemoryLockService.requestReadLock(lockNameType, lockName, null, releaseOnCommit);
        } catch (LockManagerTimeoutException e) {
            LOGGER.warn("Write lock '{}' held by another thread: " + e.getMessage(), lockName);
            throw e;
        }
    }

    @Override
    public void requestReadLock(String lockNameType, String lockName, Integer lockTimeout, boolean releaseOnCommit) throws LockManagerTimeoutException {
        try {
            inMemoryLockService.requestReadLock(lockNameType, lockName, lockTimeout, releaseOnCommit);
        } catch (LockManagerTimeoutException e) {
            LOGGER.warn("Write lock '{}' held by another thread: " + e.getMessage(), lockName);
            throw e;
        }
    }

    @Override
    public void requestWriteLock(String lockNameType, boolean releaseOnCommit) throws LockManagerTimeoutException {
        try {
            inMemoryLockService.requestWriteLock(lockNameType, null, null, releaseOnCommit);
        } catch (LockManagerTimeoutException e) {
            LOGGER.warn("Write or read lock '{}' held by another thread: " + e.getMessage(), lockNameType);
            throw e;
        }
    }

    @Override
    public void requestWriteLock(String lockNameType, String lockName, boolean releaseOnCommit) throws LockManagerTimeoutException {
        try {
            inMemoryLockService.requestWriteLock(lockNameType, lockName, null, releaseOnCommit);
        } catch (LockManagerTimeoutException e) {
            LOGGER.warn("Write or read lock '{}' held by another thread: " + e.getMessage(), lockName);
            throw e;
        }
    }

    @Override
    public void requestWriteLock(String lockNameType, String lockName, Integer lockTimeout, boolean releaseOnCommit) throws LockManagerTimeoutException {
        try {
            inMemoryLockService.requestWriteLock(lockNameType, lockName, lockTimeout, releaseOnCommit);
        } catch (LockManagerTimeoutException e) {
            LOGGER.warn("Write or read lock '{}' held by another thread: " + e.getMessage(), lockName);
            throw e;
        }
    }

    @Override
    public void unlock(String lockNameType) {
        inMemoryLockService.unLock(lockNameType, null);
    }

    @Override
    public void unlock(String lockNameType, String lockName) {
        inMemoryLockService.unLock(lockNameType, lockName);
    }

    /**
     * Converts a lock from one mode to another mode according to parameters
     *
     * @param lockNameType lock name type
     * @throws LockManagerTimeoutException If the lock cannot be granted within this time period.
     * @throws LockManagerRunTimeException internal exception
     */
    public void convertToReadLock(String lockNameType) throws LockManagerTimeoutException {
        convertToReadLock(lockNameType, null, null);
    }

    /**
     * Converts a lock from one mode to another mode according to parameters
     *
     * @param lockNameType lock name type
     * @param lockName     lock name
     * @throws LockManagerTimeoutException If the lock cannot be granted within this time period.
     * @throws LockManagerRunTimeException internal exception
     */
    public void convertToReadLock(String lockNameType, String lockName) throws LockManagerTimeoutException {
        convertToReadLock(lockNameType, lockName, null);
    }

    @Override
    public void convertToReadLock(String lockNameType, String lockName, Integer lockTimeout) throws LockManagerTimeoutException {
        try {
            inMemoryLockService.convertToReadLock(lockNameType, lockName, lockTimeout);
        } catch (LockManagerTimeoutException e) {
            LOGGER.warn("Write lock '{}' held by another thread: " + e.getMessage(), lockName);
            throw e;
        }
    }

    /**
     * Converts a lock from one mode to another mode according to parameters
     *
     * @param lockNameType lock name type
     * @throws LockManagerTimeoutException If the lock cannot be granted within this time period.
     * @throws LockManagerRunTimeException internal exception
     */
    public void convertToWriteLock(String lockNameType) throws LockManagerTimeoutException {
        convertToWriteLock(lockNameType, null, null);
    }

    /**
     * Converts a lock from one mode to another mode according to parameters
     *
     * @param lockNameType lock name type
     * @param lockName     lock name
     * @throws LockManagerTimeoutException If the lock cannot be granted within this time period.
     * @throws LockManagerRunTimeException internal exception
     */
    public void convertToWriteLock(String lockNameType, String lockName) throws LockManagerTimeoutException {
        convertToWriteLock(lockNameType, lockName, null);
    }

    @Override
    public void convertToWriteLock(String lockNameType, String lockName, Integer lockTimeout) throws LockManagerTimeoutException {
        try {
            inMemoryLockService.convertToWriteLock(lockNameType, lockName, lockTimeout);
        } catch (LockManagerTimeoutException e) {
            LOGGER.warn("Write or read lock '{}' held by another thread: " + e.getMessage(), lockName);
            throw e;
        }
    }
}
