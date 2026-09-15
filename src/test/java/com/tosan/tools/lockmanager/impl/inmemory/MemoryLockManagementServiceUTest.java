package com.tosan.tools.lockmanager.impl.inmemory;

import com.tosan.tools.lockmanager.exception.LockManagerTimeoutException;
import com.tosan.tools.lockmanager.impl.memory.MemoryLockManagementService;
import com.tosan.tools.lockmanager.impl.memory.MemoryLockService;
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
public class MemoryLockManagementServiceUTest {
    private static final String LOCK_NAME_TYPE = "NAME";
    private static final String LOCK_NAME = "TEST";

    private MemoryLockManagementService memoryLockManagementService;
    private ExecutorService executorService;

    @BeforeEach
    public void setup() {
        memoryLockManagementService = new MemoryLockManagementService(new MemoryLockService());
        executorService = Executors.newSingleThreadExecutor();
    }

    @AfterEach
    public void clean() {
        executorService.shutdownNow();
    }

    @Test
    public void memory_requestReadLockAndReleaseIt_success() {
        memoryLockManagementService.requestReadLock(LOCK_NAME_TYPE, LOCK_NAME, 0, true);
        memoryLockManagementService.unlock(LOCK_NAME_TYPE, LOCK_NAME);
    }

    @Test
    public void inMemory_requestWriteLockAndReleaseIt_success() {
        memoryLockManagementService.requestWriteLock(LOCK_NAME_TYPE, LOCK_NAME, 0, true);
        memoryLockManagementService.unlock(LOCK_NAME_TYPE, LOCK_NAME);
    }

    @Test
    public void memory_requestWriteLockAndConvertItToReadLockThenReleaseIt_success() {
        memoryLockManagementService.requestWriteLock(LOCK_NAME_TYPE, LOCK_NAME, 0, true);
        memoryLockManagementService.convertToReadLock(LOCK_NAME_TYPE, LOCK_NAME, 0);
        memoryLockManagementService.unlock(LOCK_NAME_TYPE, LOCK_NAME);
    }

    @Test
    public void memory_requestReadLockAndConvertItToWriteLockThenReleaseIt_success() {
        memoryLockManagementService.requestReadLock(LOCK_NAME_TYPE, LOCK_NAME, 0, true);
        memoryLockManagementService.convertToWriteLock(LOCK_NAME_TYPE, LOCK_NAME, 0);
        memoryLockManagementService.unlock(LOCK_NAME_TYPE, LOCK_NAME);
    }

    @Test
    public void memory_requestWriteLockWithDifferentThreads_timeoutException() throws ExecutionException, InterruptedException {
        Runnable holdWriteLock = () -> memoryLockManagementService.requestWriteLock(LOCK_NAME_TYPE, LOCK_NAME, 0, true);
        executorService.submit(holdWriteLock).get();
        assertThrows(LockManagerTimeoutException.class,
                () -> memoryLockManagementService.requestWriteLock(LOCK_NAME_TYPE, LOCK_NAME, 1, true));
        executorService.submit(() -> memoryLockManagementService.unlock(LOCK_NAME_TYPE, LOCK_NAME)).get();
    }

    @Test
    public void memory_unlockFromNonOwnerThread_shouldNotReleaseTheLock() throws Exception {
        Runnable holdWriteLock = () -> memoryLockManagementService.
                requestWriteLock(LOCK_NAME_TYPE, LOCK_NAME, 0, true);
        executorService.submit(holdWriteLock).get();
        memoryLockManagementService.unlock(LOCK_NAME_TYPE, LOCK_NAME);
        assertThrows(LockManagerTimeoutException.class,
                () -> memoryLockManagementService.requestWriteLock(LOCK_NAME_TYPE, LOCK_NAME, 1, true));
        executorService.submit(() -> memoryLockManagementService.unlock(LOCK_NAME_TYPE, LOCK_NAME)).get();
    }

    @Test
    public void memory_multipleThreadsHoldReadLockConcurrently_success() throws Exception {
        memoryLockManagementService.requestReadLock(LOCK_NAME_TYPE, LOCK_NAME, 0, true);
        Runnable acquireReadLockToo = () -> memoryLockManagementService.requestReadLock(LOCK_NAME_TYPE, LOCK_NAME, 1, true);
        executorService.submit(acquireReadLockToo).get();
        memoryLockManagementService.unlock(LOCK_NAME_TYPE, LOCK_NAME);
    }
}
