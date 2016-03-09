// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.jni.filelock;

import java.nio.channels.FileLock;

import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.Platform;

/**
 * A {@link FileLock} will create an (advisory) lock for a local path to a file
 * or directory (that need not necessarily exist.) This class is simply a facade
 * around the underlying system-level {@link IFileLock} implementations.
 *
 * @threadsafety unknown
 */
public class TFSFileLock implements ITFSFileLock {
    private final ITFSFileLock fileLock;

    /**
     * Creates a file lock for the given file or directory. This file need not
     * exist. Depending on the underlying implementation, the parent directory
     * may be created.
     *
     * @param filename
     *        The local path to the file to be locked
     * @throws TFSFileLockException
     *         if the file lock could not be created
     */
    public TFSFileLock(final String filename) {
        this(filename, false);
    }

    /**
     * Creates a file lock for the given file or directory. This file need not
     * exist. Depending on the underlying implementation, the parent directory
     * may be created. May optionally acquire the lock in a blocking manner.
     *
     * @param filename
     *        The local path to the file to be locked
     * @param acquire
     *        <code>true</code> to acquire the lock, <code>false</code>
     *        otherwise
     * @throws TFSFileLockException
     *         if the file lock could not be created
     */
    public TFSFileLock(final String filename, final boolean acquire) {
        Check.notNull(filename, "filename"); //$NON-NLS-1$

        if (Platform.isCurrentPlatform(Platform.WINDOWS)) {
            fileLock = new WindowsMutexFileLock(filename);
        } else {
            fileLock = new NIOFileLock(filename);
        }

        Check.notNull(fileLock, "fileLock"); //$NON-NLS-1$

        if (acquire) {
            acquire();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getFilename() {
        return fileLock.getFilename();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void acquire() {
        fileLock.acquire();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean acquire(final int timeout) {
        return fileLock.acquire(timeout);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void release() {
        fileLock.release();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {
        fileLock.close();
    }
}
