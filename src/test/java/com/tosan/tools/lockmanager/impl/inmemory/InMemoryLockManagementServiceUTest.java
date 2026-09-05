package com.tosan.tools.lockmanager.impl.inmemory;

import com.tosan.tools.lockmanager.exception.LockManagerTimeoutException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @since 28/08/2026
 */
public class InMemoryLockManagementServiceUTest {
    private static final String LOCK_NAME_TYPE = "NAME";
    private static final String LOCK_NAME = "TEST";

    private InMemoryLockManagementService inMemoryLockManagementService;
    private ExecutorService executorService;

    @BeforeEach
    public void setup() {
        inMemoryLockManagementService = new InMemoryLockManagementService(new InMemoryLockService());
        executorService = Executors.newSingleThreadExecutor();
    }

    @AfterEach
    public void clean() {
        executorService.shutdownNow();
    }

    @Test
    public void inMemory_requestReadLockAndReleaseIt_success() {
        inMemoryLockManagementService.requestReadLock(LOCK_NAME_TYPE, LOCK_NAME, 0, true);
        inMemoryLockManagementService.unlock(LOCK_NAME_TYPE, LOCK_NAME);
    }

    @Test
    public void inMemory_requestWriteLockAndReleaseIt_success() {
        inMemoryLockManagementService.requestWriteLock(LOCK_NAME_TYPE, LOCK_NAME, 0, true);
        inMemoryLockManagementService.unlock(LOCK_NAME_TYPE, LOCK_NAME);
    }

    @Test
    public void inMemory_requestWriteLockAndConvertItToReadLockThenReleaseIt_success() {
        inMemoryLockManagementService.requestWriteLock(LOCK_NAME_TYPE, LOCK_NAME, 0, true);
        inMemoryLockManagementService.convertToReadLock(LOCK_NAME_TYPE, LOCK_NAME, 0);
        inMemoryLockManagementService.unlock(LOCK_NAME_TYPE, LOCK_NAME);
    }

    @Test
    public void inMemory_requestReadLockAndConvertItToWriteLockThenReleaseIt_success() {
        inMemoryLockManagementService.requestReadLock(LOCK_NAME_TYPE, LOCK_NAME, 0, true);
        inMemoryLockManagementService.convertToWriteLock(LOCK_NAME_TYPE, LOCK_NAME, 0);
        inMemoryLockManagementService.unlock(LOCK_NAME_TYPE, LOCK_NAME);
    }

    @Test
    public void inMemory_requestWriteLockWithDifferentThreads_timeoutException() throws ExecutionException, InterruptedException {
        Runnable holdWriteLock = () -> inMemoryLockManagementService.requestWriteLock(LOCK_NAME_TYPE, LOCK_NAME, 0, true);
        executorService.submit(holdWriteLock).get();
        assertThrows(LockManagerTimeoutException.class,
                () -> inMemoryLockManagementService.requestWriteLock(LOCK_NAME_TYPE, LOCK_NAME, 1, true));
        executorService.submit(() -> inMemoryLockManagementService.unlock(LOCK_NAME_TYPE, LOCK_NAME)).get();
    }

    @Test
    public void inMemory_unlockFromNonOwnerThread_shouldNotReleaseTheLock() throws Exception {
        Runnable holdWriteLock = () -> inMemoryLockManagementService.
                requestWriteLock(LOCK_NAME_TYPE, LOCK_NAME, 0, true);
        executorService.submit(holdWriteLock).get();
        inMemoryLockManagementService.unlock(LOCK_NAME_TYPE, LOCK_NAME);
        assertThrows(LockManagerTimeoutException.class,
                () -> inMemoryLockManagementService.requestWriteLock(LOCK_NAME_TYPE, LOCK_NAME, 1, true));
        executorService.submit(() -> inMemoryLockManagementService.unlock(LOCK_NAME_TYPE, LOCK_NAME)).get();
    }

    @Test
    public void inMemory_multipleThreadsHoldReadLockConcurrently_success() throws Exception {
        inMemoryLockManagementService.requestReadLock(LOCK_NAME_TYPE, LOCK_NAME, 0, true);
        Runnable acquireReadLockToo = () -> inMemoryLockManagementService.requestReadLock(LOCK_NAME_TYPE, LOCK_NAME, 1, true);
        executorService.submit(acquireReadLockToo).get();
        inMemoryLockManagementService.unlock(LOCK_NAME_TYPE, LOCK_NAME);
    }
}
