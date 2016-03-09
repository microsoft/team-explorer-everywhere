// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.jni.filelock;

/**
 * <p>
 * An advisory file locking mechanism that will lock a file both across threads
 * within the JVM and for other processes on the system. The underlying file
 * lock mechanism is implemented on a per-platform basis.
 * </p>
 *
 * <p>
 * This is intended to use compatible file locking with other TFS clients, thus
 * this mechanism uses system mutexes compatible with Visual Studio clients. On
 * Unix platforms, this uses {@link java.nio.channels.FileLock}s, however users
 * should not expect a guarantee of implementation as this may change over time
 * for correctness or compatibility.
 * </p>
 *
 * <p>
 * Regardless of underlying implementation, this mechanism always uses Windows
 * Mutex-like semantics. A single thread can acquire a lock on a file which will
 * block both other threads on the same JVM and other JVMs on the same computer.
 * When a thread acquires a lock on a file, the thread can take additional locks
 * (by calling {@link #acquire()} again). The lock is not released back to the
 * system until the lock is released once for each acquisition. A file lock
 * object is a handle to a lock - two objects referring to the same lock file
 * are considered equivalent.
 * </p>
 *
 * <p>
 * Locking over a network file system (NFS, SMB) is not guaranteed and is, in
 * fact, unlikely to function correctly.
 * </p>
 *
 * @threadsafety thread safe
 */
public interface ITFSFileLock {
    public static final int WAIT_INFINITE = -1;

    String getFilename();

    /**
     * Acquires a lock on this file. If another thread or process has a lock to
     * this file, this method will block until that lock is released.
     *
     * @throws TFSFileLockException
     *         if there was an error with the file lock
     */
    void acquire();

    /**
     * Attempts to acquire a lock on this file. If another thread or process has
     * a lock to this file, this method will block until that lock is released
     * or until the given timeout has elapsed, whichever comes first.
     *
     * @param timeout
     *        the number of milliseconds to wait while trying to acquire the
     *        lock
     * @throws TFSFileLockException
     *         if there was an error with the file lock
     * @return <code>true</code> if the lock was acquired successfully,
     *         <code>false</code> if the acquisition timed out
     */
    boolean acquire(int timeout);

    /**
     * Releases the lock on this file that was previously taken with acquire. If
     * this is the last lock on the file for this thread, the lock is released
     * to the system.
     *
     * @throws TFSFileLockException
     *         if the lock could not be released or if the current thread does
     *         not own the lock
     */
    void release();

    /**
     * Closes this handle to the lock. This object is now invalid and cannot be
     * acquired or released. You may call close multiple times.
     */
    void close();
}
