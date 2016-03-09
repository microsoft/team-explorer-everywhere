// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.jni;

/**
 * <p>
 * Represents a system-wide ("named") synchronization objects.
 * </p>
 *
 * <p>
 * Semaphores represent synchronization objects that maintain a count between
 * zero and an initial value. (The initial value is also the maximum value.)
 * This count is decremented each time a thread completes a wait for the
 * semaphore object and incremented each time a thread releases the semaphore.
 * When the count reaches zero, callers may not wait for the semaphore until
 * another caller releases the object.
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
    /* Mutexes */

    /**
     * Creates a semaphore with the given name.
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

    /* Semaphores */

    /**
     * Creates a semaphore with the given name, and the given initial value.
     *
     * @param name
     *        The name of the semaphore to create
     * @param initialValue
     *        The initial value for the semaphore (must be greater than
     *        <code>0</code>). This initial value is also the maximum value.
     * @return A semaphore ID suitable for calling other semaphore functions
     *         with on success, <code>-1</code> on failure.
     */
    public long createSemaphore(final String name, final int initialValue);

    /**
     * Acquires the given semaphore or blocks for the given timeout if the
     * semaphore is not available. In terms of implementation, this method
     * blocks until the semaphore's value is greater than zero, then reduces the
     * value by one.
     *
     * @param semaphoreId
     *        The semaphore ID to acquire
     * @param timeout
     *        The time (in milliseconds) to block while attempting to acquire
     *        the semaphore, or <code>-1</code> to block forever
     * @return <code>1</code> if the semaphore was acquired, <code>0</code> if
     *         acquisition would block, <code>-1</code> if there was a failure
     */
    int waitForSemaphore(final long semaphoreId, final int timeout);

    /**
     * Releases the semaphore. In terms of implementation, this method
     * increments the semaphore's value by one. No checks are performed to
     * ensure that the caller previously called {@link #waitForSemaphore(long)}
     * or {@link #tryWaitForSemaphore(long)}, thus callers must check return
     * values of those other functions.
     *
     * @param semaphoreId
     *        The semaphore ID to release
     * @return <code>true</code> if the semaphore was released,
     *         <code>false</code> if there was a failure
     */
    boolean releaseSemaphore(final long semaphoreId);

    /**
     * Closes the semaphore. Allows the operating system the opportunity to
     * reclaim resources used by the semaphore. Subsequent calls to
     * waitForSemaphore or releaseSemaphore will fail.
     *
     * @param semaphoreId
     *        The semaphore to close
     * @return <code>true</code> if the semaphore was closed, <code>false</code>
     *         if there was a failure
     */
    boolean closeSemaphore(final long semaphoreId);
}
