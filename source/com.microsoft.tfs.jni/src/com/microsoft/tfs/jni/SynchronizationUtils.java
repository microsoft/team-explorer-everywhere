// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.jni;

import com.microsoft.tfs.jni.internal.synchronization.NativeSynchronization;

/**
 * Represents a system-wide ("named") mutex or semaphore. It is recommended that
 * callers use an object-oriented wrapper instead.
 */
public class SynchronizationUtils implements Synchronization {
    private static final SynchronizationUtils instance = new SynchronizationUtils();

    /**
     * @return the system's synchronization utils
     */
    public static SynchronizationUtils getInstance() {
        return SynchronizationUtils.instance;
    }

    private final NativeSynchronization nativeImpl;

    private SynchronizationUtils() {
        nativeImpl = new NativeSynchronization();
    }

    /* Mutexes */

    @Override
    public long createMutex(final String name) {
        return nativeImpl.createMutex(name);
    }

    @Override
    public int waitForMutex(final long mutexId, final int timeout) {
        return nativeImpl.waitForMutex(mutexId, timeout);
    }

    @Override
    public boolean releaseMutex(final long mutexId) {
        return nativeImpl.releaseMutex(mutexId);
    }

    @Override
    public boolean closeMutex(final long mutexId) {
        return nativeImpl.closeMutex(mutexId);
    }

    /* Semaphores */

    @Override
    public long createSemaphore(final String name, final int initialValue) {
        return nativeImpl.createSemaphore(name, initialValue);
    }

    @Override
    public int waitForSemaphore(final long semaphoreId, final int timeout) {
        return nativeImpl.waitForSemaphore(semaphoreId, timeout);
    }

    @Override
    public boolean releaseSemaphore(final long semaphoreId) {
        return nativeImpl.releaseSemaphore(semaphoreId);
    }

    @Override
    public boolean closeSemaphore(final long semaphoreId) {
        return nativeImpl.closeSemaphore(semaphoreId);
    }
}