// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.jni.filelock;

import java.io.File;
import java.util.regex.Pattern;

import com.microsoft.tfs.jni.SynchronizationUtils;
import com.microsoft.tfs.util.Check;

/**
 * <p>
 * An implementation of {@link IFileLock} that uses Windows Mutex objects to
 * synchronize.
 * </p>
 * <p>
 * The underlying mutex is intended to match the mutexes used for Visual Studio
 * local metadata table locks - that is, a mutex created by this class will be a
 * global mutex (that is, one that spans Terminal Services sessions) and will
 * have a name that is the given filename, with path separators converted to
 * underscores.
 * </p>
 *
 * @threadsafety unknown
 */
final class WindowsMutexFileLock implements ITFSFileLock {
    /*
     * This prefix is added to ensure that our mutexes are "global" meaning all
     * Terminal Services sessions for the current user. Visual Studio does this
     * as well and this is required for matching.
     */
    private static final String GLOBAL_PREFIX = "Global\\"; //$NON-NLS-1$

    private final String filename;
    private final String mutexName;

    private volatile long mutexID;

    WindowsMutexFileLock(final String filename) {
        Check.notNull(filename, "filename"); //$NON-NLS-1$

        /*
         * Translate the file name like the Visual Studio client: translate file
         * separators into underscores.
         */
        this.filename = filename;
        this.mutexName = GLOBAL_PREFIX + filename.replaceAll(Pattern.quote(File.separator), "_"); //$NON-NLS-1$

        this.mutexID = SynchronizationUtils.getInstance().createMutex(mutexName);

        if (mutexID < 0) {
            throw new TFSFileLockException("Could not create system mutex"); //$NON-NLS-1$
        }
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
    public boolean acquire(final int timeout) {
        if (mutexID < 0) {
            throw new TFSFileLockException("This file lock is closed"); //$NON-NLS-1$
        }

        final int result = SynchronizationUtils.getInstance().waitForMutex(mutexID, timeout);

        if (result < 0) {
            throw new TFSFileLockException("Could not acquire mutex"); //$NON-NLS-1$
        }

        return (result == 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void release() {
        if (mutexID < 0) {
            throw new TFSFileLockException("This file lock is closed"); //$NON-NLS-1$
        }

        if (!SynchronizationUtils.getInstance().releaseMutex(mutexID)) {
            throw new TFSFileLockException("Could not release mutex"); //$NON-NLS-1$
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {
        /* It is acceptable to call {@link #close} multiple times. */
        if (mutexID < 0) {
            return;
        }

        if (!SynchronizationUtils.getInstance().closeMutex(mutexID)) {
            throw new TFSFileLockException("Could not close mutex"); //$NON-NLS-1$
        }

        mutexID = -1;
    }
}
