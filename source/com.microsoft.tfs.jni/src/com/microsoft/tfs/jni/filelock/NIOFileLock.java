// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.jni.filelock;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.microsoft.tfs.util.Check;

/**
 * A file lock implementation that uses a static data map to arbitrate locking
 * between threads and Java NIO file locks to arbitrate locking between
 * processes.
 *
 * @threadsafety unknown
 */
class NIOFileLock implements ITFSFileLock {
    private static final String FILE_SUFFIX = ".lock"; //$NON-NLS-1$

    private static final int POLL_TIME = 100;

    private final String filename;
    private final File lockFile;

    /*
     * Closed refers to whether this particular object has been closed. Callers
     * may close this reference to the underlying file lock, which will NOT
     * release the lock if it is held. (Other references within the same thread
     * then take over any acquisitions this object has made.)
     */
    private volatile boolean closed = false;

    /*
     * We keep a thread local map of lock files to data containing the
     * acquisition count. This allows us to acquire this lock multiple times on
     * a single thread without blocking or error (and avoids messy
     * synchronization.)
     */
    private static final ThreadLocal<Map<File, NIOFileLockData>> lockFileToDataMap =
        new ThreadLocal<Map<File, NIOFileLockData>>() {
            @Override
            protected Map<File, NIOFileLockData> initialValue() {
                return new HashMap<File, NIOFileLockData>();
            }
        };

    /*
     * We also keep a list of the locks across the JVM - file locks are on a
     * per-process basis, and this will provide us with granularity to the
     * thread level.
     */
    private static final Set<File> lockFiles = new HashSet<File>();

    NIOFileLock(final String filename) {
        Check.notNull(filename, "filename"); //$NON-NLS-1$

        this.filename = filename;

        final File targetFile = new File(filename);
        final File parentFile = targetFile.getParentFile();

        /* Ensure that parent folders exist. */
        if (!parentFile.exists() && !parentFile.mkdirs()) {
            throw new TFSFileLockException(
                MessageFormat.format("Cannot create directory {0}", parentFile.getAbsolutePath())); //$NON-NLS-1$
        }

        this.lockFile = new File(targetFile.getAbsoluteFile() + FILE_SUFFIX);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getFilename() {
        return filename;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void acquire() {
        if (!acquire(WAIT_INFINITE)) {
            throw new TFSFileLockException("Could not acquire mutex"); //$NON-NLS-1$
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean acquire(int timeout) {
        if (closed) {
            throw new TFSFileLockException("This file lock has been closed."); //$NON-NLS-1$
        }

        boolean success = false;

        /* See if this thread already has this lock. */
        final NIOFileLockData fileLockData = lockFileToDataMap.get().get(lockFile);

        if (fileLockData != null) {
            /* Sanity check */
            if (fileLockData.getAcquisitionCount() <= 0) {
                throw new TFSFileLockException(MessageFormat.format("Invalid file lock state for file {0}", filename)); //$NON-NLS-1$
            }

            /* Increment the use count. */
            fileLockData.acquire();
            return true;
        }

        /* Set up timeouts */
        if (timeout < 0) {
            timeout = Integer.MAX_VALUE;
        }

        try {
            /*
             * This thread does not have the lock. Try to get the JVM-wide lock
             * for this file.
             */
            do {
                synchronized (lockFiles) {
                    if (!lockFiles.contains(lockFile)) {
                        /* No other thread has this lock, we can take it. */
                        lockFiles.add(lockFile);

                        success = true;
                        break;
                    }
                }

                final int sleepTime = timeout < POLL_TIME ? timeout : POLL_TIME;
                Thread.sleep(sleepTime);
                timeout -= sleepTime;
            } while (timeout > 0);
        } catch (final InterruptedException e) {
            throw new TFSFileLockException("Could not acquire file lock", e); //$NON-NLS-1$
        }

        /*
         * See if we fell out of the loop because we timed out waiting to
         * acquire the lock.
         */
        if (!success) {
            return false;
        }

        /*
         * We now have the JVM wide lock. Use NIO locking to try to acquire a
         * system-wide lock.
         */
        success = false;
        RandomAccessFile raFile = null;

        try {
            raFile = new RandomAccessFile(lockFile, "rw"); //$NON-NLS-1$

            do {
                /*
                 * The random access file gives us filesystem locking on Unix
                 * (note: this is mandatory locking on Windows, where this class
                 * is not expected to be used.)
                 */
                final FileLock lock = raFile.getChannel().tryLock();

                if (lock != null) {
                    /*
                     * Success: we have acquire all locks, set up the
                     * thread-level lock data so that we have an acquisition
                     * count.
                     */
                    lockFileToDataMap.get().put(lockFile, new NIOFileLockData(raFile, lock));

                    success = true;
                    break;
                }

                final int sleepTime = timeout < POLL_TIME ? timeout : POLL_TIME;
                Thread.sleep(sleepTime);
                timeout -= sleepTime;
            } while (timeout > 0);
        } catch (final Exception e) {
            throw new TFSFileLockException("Could not acquire file lock", e); //$NON-NLS-1$
        } finally {
            if (!success) {
                /* Close the random access file if we were able to open it. */
                if (raFile != null) {
                    try {
                        raFile.close();
                    } catch (final IOException e) {
                        /* Suppress */
                    }
                }

                /*
                 * We do not have a complete lock, thus we must unwind our
                 * JVM-wide lock data.
                 */
                synchronized (lockFiles) {
                    lockFiles.remove(lockFile);
                }
            }
        }

        return success;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void release() {
        if (closed) {
            throw new TFSFileLockException("This file lock has been closed."); //$NON-NLS-1$
        }

        /* Ensure this thread has the lock. */
        final NIOFileLockData fileLockData = lockFileToDataMap.get().get(lockFile);

        if (fileLockData == null) {
            throw new TFSFileLockException(
                MessageFormat.format("The lock for file {0} is not held by the calling thread", filename)); //$NON-NLS-1$
        }

        /* Decrement the use count for this thread. */
        fileLockData.release();

        if (fileLockData.getAcquisitionCount() < 0) {
            /* Sanity check */
            throw new TFSFileLockException(MessageFormat.format("Inconsistent file lock state for {0}", filename)); //$NON-NLS-1$
        }

        /* If this thread is still using this lock elsewhere, simply return. */
        if (fileLockData.getAcquisitionCount() > 0) {
            return;
        }

        /*
         * This thread is no longer using this file lock, release it to the
         * wild.
         */
        try {
            /*
             * We must release the lock before closing the file on Mac OS and
             * Linux.
             */
            fileLockData.getFileLock().release();
            fileLockData.getRandomAccessFile().close();
        } catch (final Exception e) {
            throw new TFSFileLockException("Could not release file lock", e); //$NON-NLS-1$
        } finally {
            lockFileToDataMap.get().remove(lockFile);
            lockFiles.remove(lockFile);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {
        this.closed = true;
    }

    private static class NIOFileLockData {
        private final RandomAccessFile randomAccessFile;
        private final FileLock fileLock;

        private int acquisitionCount;

        public NIOFileLockData(final RandomAccessFile randomAccessFile, final FileLock fileLock) {
            Check.notNull(randomAccessFile, "randomAccessFile"); //$NON-NLS-1$
            Check.notNull(fileLock, "fileLock"); //$NON-NLS-1$

            this.randomAccessFile = randomAccessFile;
            this.fileLock = fileLock;
            this.acquisitionCount = 1;
        }

        public RandomAccessFile getRandomAccessFile() {
            return randomAccessFile;
        }

        public FileLock getFileLock() {
            return fileLock;
        }

        public int getAcquisitionCount() {
            return acquisitionCount;
        }

        public void acquire() {
            acquisitionCount++;
        }

        public void release() {
            acquisitionCount--;
        }
    }
}
