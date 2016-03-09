// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.localworkspace;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.jni.filelock.TFSFileLock;
import com.microsoft.tfs.util.Check;

/**
 * @threadsafety unknown
 */
public class LocalMetadataTableLock {
    // The name of the yield request mutex
    private final String yieldRequestLockName;

    // The file lock object (metadata table lock)
    private final TFSFileLock fileLock;

    // Indicates whether m_mutex is held
    private boolean holdsFileLock;

    // Number of times to spin when trying to acquire the mutex
    private final int retryCount;

    public LocalMetadataTableLock(final String filename) {
        this(filename, 7, false);
    }

    public LocalMetadataTableLock(final String fileName, final boolean requestYield) {
        this(fileName, 7, requestYield);
    }

    public LocalMetadataTableLock(final String filename, final int retryCount, final boolean requestYield) {
        Check.notNull(filename, "filename"); //$NON-NLS-1$

        this.yieldRequestLockName = filename + ";yield"; //$NON-NLS-1$
        this.retryCount = retryCount;

        this.fileLock = new TFSFileLock(filename, false);

        acquire(requestYield);
    }

    /**
     * Acquires the mutex corresponding to this LocalMetadataTableLock.
     *
     *
     * @param requestYield
     *        If true, then iff the lock cannot be obtained immediately, this
     *        thread will attempt to acquire the yield request mutex to signal
     *        to the holding thread that it should yield the mutex at its
     *        earliest opportunity.
     */
    private void acquire(final boolean requestYield) {
        int retryCount = 0;
        boolean holdsRequestYieldLock = false;
        TFSFileLock requestYieldLock = null;

        try {
            if (fileLock.acquire(0)) {
                holdsFileLock = true;
            } else if (requestYield) {
                requestYieldLock = new TFSFileLock(yieldRequestLockName, false);

                if (requestYieldLock.acquire(0)) {
                    holdsRequestYieldLock = true;
                }
            }

            while (!holdsFileLock && retryCount++ < this.retryCount) {
                if (fileLock.acquire(200 * retryCount * retryCount)) {
                    holdsFileLock = true;
                } else if (requestYield && !holdsRequestYieldLock) {
                    if (requestYieldLock.acquire(0)) {
                        holdsRequestYieldLock = true;
                    }
                }
            }

            // If we couldn't get the mutex within our timeout window, throw.
            if (!holdsFileLock) {
                throw new LocalMetadataTableTimeoutException(
                    Messages.getString("LocalMetadataTableLock.LocalMetadataTableMutexTimeout")); //$NON-NLS-1$
            }
        } finally {
            if (null != requestYieldLock) {
                if (holdsRequestYieldLock) {
                    requestYieldLock.release();
                }
                requestYieldLock.close();
            }
        }
    }

    public void close() {
        if (this.fileLock != null) {
            if (holdsFileLock) {
                fileLock.release();
            }

            fileLock.close();
        }
    }

    /**
     * Returns true if another thread in the system is currently holding the
     * yield request lock for this LocalMetadataTableLock, indicating that we
     * should yield if we have the opportunity.
     */
    public boolean isYieldRequested() {
        boolean holdsLock = false;
        final TFSFileLock requestYieldLock = new TFSFileLock(yieldRequestLockName, false);

        try {
            if (requestYieldLock.acquire(0)) {
                holdsLock = true;
            }

            // If we could not obtain the request yield lock, then someone is
            // requesting a yield.
            return !holdsLock;
        } finally {
            if (null != requestYieldLock) {
                if (holdsLock) {
                    requestYieldLock.release();
                }

                requestYieldLock.close();
            }
        }
    }

    /**
     * Releases and re-acquires the LocalMetadataTableLock. When the method
     * returns, the lock has been re-acquired.
     */
    public void yield() {
        Check.isTrue(holdsFileLock, "holdsFileLock"); //$NON-NLS-1$

        if (null != fileLock) {
            if (holdsFileLock) {
                fileLock.release();
                holdsFileLock = false;
            }

            // Yield point
            acquire(true);
        }
    }

    public class LocalMetadataTableTimeoutException extends RuntimeException {
        private static final long serialVersionUID = 2607330749072651597L;

        public LocalMetadataTableTimeoutException(final String message) {
            super(message);
        }
    }
}
