// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.util.locking;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.text.MessageFormat;
import java.util.HashSet;
import java.util.Set;

import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.Messages;

/**
 * <p>
 * Provides advisory locking (at minimum, some platforms may escalate to
 * mandatory) of a filesystem resource. The lock can be held by at most one
 * thread in a VM, and by only one VM across the computer. This is an
 * alternative to {@link FileLock}, which is held by all threads in a virtual
 * machine.
 * </p>
 * <p>
 * When the lock is created, it is initially held (locked). Make sure to call
 * {@link #release()} to release the lock.
 * </p>
 * <p>
 * Locks may be used one time only. Once a lock is released it may not be
 * re-acquired. Call {@link #create(File, boolean)} for a new lock.
 * </p>
 *
 * @since TEE-SDK-10.1
 * @threadsafety thread-safe
 */
public final class AdvisoryFileLock {
    /**
     * This class uses {@link FileLock}s for its system-wide lock facility. A
     * {@link FileLock} can only be owned once among all virtual machines on a
     * computer, but when one virtual machine has a thread that has the lock,
     * all threads in the virtual machine also have the lock. We want a file
     * lock to be per-thread, so we do extra synchronization to accomplish that.
     *
     * The objects in the set are the lock target {@link File}s that are
     * currently locked.
     */
    private final static Set heldLocks = new HashSet();

    /**
     * The {@link File} the lock is mediating access to.
     */
    private final File lockFile;

    /**
     * The system-wide lock object, created from {@link #raf} to acquire/release
     * the lock.
     */
    private final FileLock lock;

    /**
     * The {@link RandomAccessFile} used to create the {@link FileLock} that
     * provides system-wide locking. We have to keep the
     * {@link RandomAccessFile} around because it must be closed after the
     * {@link FileLock} is released, not before.
     */
    private final RandomAccessFile raf;

    /**
     * Set to true upon first release to prevent double release, which causes
     * lock state problems in Java 6. Symptoms include occasional
     * {@link OverlappingFileLockException} when finalized by the garbage
     * collector.
     *
     * Access to this field must be synchronized on the class instance.
     */
    private boolean released = false;

    /**
     * Creates an {@link AdvisoryFileLock} that mediates access to the given
     * file path. Blocks forever (or until thread interrupted) if block is true,
     * returns immediately if block if false.
     *
     * @param lockFile
     *        the file to lock (not null). This file does not have to exist, but
     *        it will be created (0 bytes) if it does not
     * @param block
     *        if true, this method does not return until the lock is obtained
     *        (or the controlling thread is interrupted). If false, the method
     *        returns immediately; the value is null if the lock was not
     *        immediately available or an {@link AdvisoryFileLock} if it was.
     * @return an {@link AdvisoryFileLock}, initially owned. Returns null if and
     *         only if block was set to false and the lock was not immediately
     *         available.
     * @throws IOException
     *         if an error occurred accessing the given lock file path on disk.
     * @throws InterruptedException
     *         if this thread was interrupted while waiting for its turn to
     *         become the only thread in this VM to lock the given file path.
     */
    public static AdvisoryFileLock create(final File lockFile, final boolean block)
        throws IOException,
            InterruptedException {
        Check.notNull(lockFile, "lockFile"); //$NON-NLS-1$

        /*
         * The random access file gives us filesystem locking (manditory on some
         * platforms, advisory on others).
         */
        final RandomAccessFile raf = new RandomAccessFile(lockFile, "rw"); //$NON-NLS-1$

        /*
         * This is the lock object we wish to acquire (initially held).
         */
        FileLock lock;

        synchronized (AdvisoryFileLock.heldLocks) {
            if (block) {
                /*
                 * If the set already contains this path, wait until it is
                 * removed. The set is notified upon removal of a path so our
                 * wait will complete.
                 */
                while (AdvisoryFileLock.heldLocks.contains(lockFile)) {
                    AdvisoryFileLock.heldLocks.wait();
                }

                /*
                 * We now have the lock on the set of paths, and our path is not
                 * in that set. It is safe to lock the path we were given
                 * through its FileChannel.
                 *
                 * WARNING: Javadoc for FileChannel.lock() says it will block
                 * until the lock is available, or throw if some lockable
                 * condition happens. Turns out this method may return null if
                 * it can't lock, instead of throwing. See this (still open in
                 * January 2010) Sun bug link:
                 *
                 * http://bugs.sun.com/view_bug.do?bug_id=6209658
                 *
                 * I can't tell if Sun plans to ever fix this, so we'll assume
                 * that all null locks in the blocking path mean the current
                 * thread was interrupted.
                 */
                lock = raf.getChannel().lock();
                if (lock == null) {
                    raf.close();

                    final String messageFormat = Messages.getString("AdvisoryFileLock.InterruptedAcquiringLockFormat"); //$NON-NLS-1$
                    final String message = MessageFormat.format(messageFormat, lockFile);
                    throw new InterruptedException(message);
                }
            } else {
                /*
                 * If the set already contains this path, we will fail fast.
                 */
                if (AdvisoryFileLock.heldLocks.contains(lockFile)) {
                    return null;
                }

                lock = raf.getChannel().tryLock();
                if (lock == null) {
                    raf.close();
                    return null;
                }
            }

            /*
             * Our lock is held, add it to the set.
             */
            AdvisoryFileLock.heldLocks.add(lockFile);
        }

        return new AdvisoryFileLock(lockFile, lock, raf);
    }

    /**
     * Creates a settings lock for the given settings lock key, valid
     * {@link FileLock}, and valid {@link RandomAccessFile}.
     *
     * @param lockFile
     *        the file being locked (not null).
     * @param lock
     *        the initially locked {@link FileLock} to wrap (not null).
     * @param raf
     *        the random access file used to hold the system lock (not null).
     */
    private AdvisoryFileLock(final File lockFile, final FileLock lock, final RandomAccessFile raf) {
        /*
         * This method doesn't contain log or trace calls because loggers and
         * tracers use AdvisoryFileLock before writing their locks, and we'd
         * have an ordering problem to solve.
         */

        Check.notNull(lockFile, "lockFile"); //$NON-NLS-1$
        Check.notNull(lock, "lock"); //$NON-NLS-1$
        Check.notNull(raf, "raf"); //$NON-NLS-1$

        this.lockFile = lockFile;
        this.lock = lock;
        this.raf = raf;
    }

    /**
     * Releases the lock so it can be acquired by other threads/processes. It is
     * safe to call this method multiple times.
     *
     * @throws IOException
     *         if an error occured releasing the underlying operating system
     *         locks.
     */
    public synchronized void release() throws IOException {
        /*
         * We must return early if we have already been released to prevent a
         * double-close/double-release.
         */
        if (released == true) {
            return;
        }

        /*
         * WISDOM: On Mac OS X 10.4, closing the RandomAccessFile before
         * releasing locks causes an IOException (about a bad file descriptor),
         * so we have to release the locks first. This ordering shouldn't cause
         * any synchronization problems since we don't care about the file
         * between lock release and close.
         *
         * On Linux with Java 6, we must also release, then close, else we get a
         * ClosedChannelException.
         */

        try {
            if (lock != null) {
                lock.release();
            }

            if (raf != null) {
                raf.close();
            }
        } finally {
            /*
             * Remove the lock and notify all waiting threads.
             */
            synchronized (AdvisoryFileLock.heldLocks) {
                if (AdvisoryFileLock.heldLocks.contains(lockFile)) {
                    AdvisoryFileLock.heldLocks.remove(lockFile);
                    AdvisoryFileLock.heldLocks.notifyAll();
                }
            }

            released = true;
        }
    }

    /**
     * Tests whether this {@link AdvisoryFileLock} has been released (via
     * {@link #release()}.
     *
     * @return true if the file lock has been released, false if it has not (the
     *         file is still locked)
     */
    public synchronized boolean isReleased() {
        return released;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void finalize() throws Throwable {
        /*
         * Release synchronizes.
         */
        release();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        /*
         * No synchronization needed because lockTarget is a final field.
         */

        return lockFile.hashCode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object obj) {
        /*
         * No synchronization needed because lockTarget is a final field.
         */

        if (obj == this) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if ((obj instanceof AdvisoryFileLock) == false) {
            return false;
        }

        final AdvisoryFileLock other = (AdvisoryFileLock) obj;

        return (other.lockFile == null) ? lockFile == null : other.lockFile.equals(lockFile);
    }
}
