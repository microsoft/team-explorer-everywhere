// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.jni;

/**
 * <p>
 * Represents a system-wide ("named") synchronization objects.
 * </p>
 *
 * <p>
 * Mutexes represent synchronization objects that are either acquired or not
 * acquired - they can be considered to be identical to semaphores with a value
 * of 1.
 * </p>
 *
 * @threadsafety unknown
 */
public interface Synchronization {

    /**
     * Creates a mutex with the given name.
     *
     * @param name
     *        The name of the mutex to create
     * @return A mutex ID suitable for calling other semaphore functions with on
     *         success, <code>-1</code> on failure.
     */
    public long createMutex(final String name);

    /**
     * Tries to acquire the given mutex in the given timeout.
     *
     * @param mutexId
     *        The mutex ID to try to acquire
     * @param timeout
     *        The time (in milliseconds) to block while attempting to acquire
     *        the mutex, or <code>-1</code> to block forever
     * @return <code>1</code> if the mutex was acquired, <code>0</code> if
     *         acquisition would block, <code>-1</code> if there was a failure
     */
    int waitForMutex(final long mutexId, final int timeout);

    /**
     * Releases the mutex. No checks are performed to ensure that the caller
     * previously called {@link #waitForMutex(long)} or
     * {@link #tryWaitForMutex(long)}, thus callers must check return values of
     * those other functions.
     *
     * @param mutexId
     *        The mutex ID to release
     * @return <code>true</code> if the mutex was released, <code>false</code>
     *         if there was a failure
     */
    boolean releaseMutex(final long mutexId);

    /**
     * Closes the mutex. Allows the operating system the opportunity to reclaim
     * resources used by the mutex. Subsequent calls to waitForMutex or
     * releaseMutex will fail.
     *
     * @param mutexId
     *        The mutex ID to close
     * @return <code>true</code> if the mutex was closed, <code>false</code> if
     *         there was a failure
     */
    boolean closeMutex(final long semaphoreId);
}
