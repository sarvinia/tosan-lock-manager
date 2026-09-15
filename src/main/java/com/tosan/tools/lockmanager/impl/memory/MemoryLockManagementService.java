package com.tosan.tools.lockmanager.impl.memory;

import com.tosan.tools.lockmanager.api.LockManagementService;
import com.tosan.tools.lockmanager.exception.LockManagerRunTimeException;
import com.tosan.tools.lockmanager.exception.LockManagerTimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MemoryLockManagementService implements LockManagementService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MemoryLockManagementService.class);
    private final MemoryLockService memoryLockService;

    public MemoryLockManagementService(MemoryLockService inMemoryLockService) {
        this.memoryLockService = inMemoryLockService;
    }

    @Override
    public void requestReadLock(String lockNameType, boolean releaseOnCommit) throws LockManagerTimeoutException {
        try {
            memoryLockService.requestReadLock(lockNameType, null, null, releaseOnCommit);
        } catch (LockManagerTimeoutException e) {
            LOGGER.warn("Write lock '{}' held by another thread: " + e.getMessage(), lockNameType);
            throw e;
        }
    }

    @Override
    public void requestReadLock(String lockNameType, String lockName, boolean releaseOnCommit) throws LockManagerTimeoutException {
        try {
            memoryLockService.requestReadLock(lockNameType, lockName, null, releaseOnCommit);
        } catch (LockManagerTimeoutException e) {
            LOGGER.warn("Write lock '{}' held by another thread: " + e.getMessage(), lockName);
            throw e;
        }
    }

    @Override
    public void requestReadLock(String lockNameType, String lockName, Integer lockTimeout, boolean releaseOnCommit) throws LockManagerTimeoutException {
        try {
            memoryLockService.requestReadLock(lockNameType, lockName, lockTimeout, releaseOnCommit);
        } catch (LockManagerTimeoutException e) {
            LOGGER.warn("Write lock '{}' held by another thread: " + e.getMessage(), lockName);
            throw e;
        }
    }

    @Override
    public void requestWriteLock(String lockNameType, boolean releaseOnCommit) throws LockManagerTimeoutException {
        try {
            memoryLockService.requestWriteLock(lockNameType, null, null, releaseOnCommit);
        } catch (LockManagerTimeoutException e) {
            LOGGER.warn("Write or read lock '{}' held by another thread: " + e.getMessage(), lockNameType);
            throw e;
        }
    }

    @Override
    public void requestWriteLock(String lockNameType, String lockName, boolean releaseOnCommit) throws LockManagerTimeoutException {
        try {
            memoryLockService.requestWriteLock(lockNameType, lockName, null, releaseOnCommit);
        } catch (LockManagerTimeoutException e) {
            LOGGER.warn("Write or read lock '{}' held by another thread: " + e.getMessage(), lockName);
            throw e;
        }
    }

    @Override
    public void requestWriteLock(String lockNameType, String lockName, Integer lockTimeout, boolean releaseOnCommit) throws LockManagerTimeoutException {
        try {
            memoryLockService.requestWriteLock(lockNameType, lockName, lockTimeout, releaseOnCommit);
        } catch (LockManagerTimeoutException e) {
            LOGGER.warn("Write or read lock '{}' held by another thread: " + e.getMessage(), lockName);
            throw e;
        }
    }

    @Override
    public void unlock(String lockNameType) {
        memoryLockService.unLock(lockNameType, null);
    }

    @Override
    public void unlock(String lockNameType, String lockName) {
        memoryLockService.unLock(lockNameType, lockName);
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
            memoryLockService.convertToReadLock(lockNameType, lockName, lockTimeout);
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
            memoryLockService.convertToWriteLock(lockNameType, lockName, lockTimeout);
        } catch (LockManagerTimeoutException e) {
            LOGGER.warn("Write or read lock '{}' held by another thread: " + e.getMessage(), lockName);
            throw e;
        }
    }
}
