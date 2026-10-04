package com.tosan.tools.lockmanager.impl.memory;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class LockWrapper {

    private final ReentrantReadWriteLock lock;
    private final ReentrantLock conversionLock;
    private final long expirationTimeNanos;

    LockWrapper(long ttl, TimeUnit timeUnit) {
        this.lock = new ReentrantReadWriteLock();
        this.conversionLock = new ReentrantLock();
        this.expirationTimeNanos =
                System.nanoTime() + timeUnit.toNanos(ttl);
    }

    ReentrantReadWriteLock getLock() {
        return lock;
    }

    ReentrantLock getConversionLock() {
        return conversionLock;
    }

    boolean isExpired() {
        return System.nanoTime() >= expirationTimeNanos;
    }
}
