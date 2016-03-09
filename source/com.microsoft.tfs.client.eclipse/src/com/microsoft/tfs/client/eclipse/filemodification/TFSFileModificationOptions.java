// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.filemodification;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.LockLevel;
import com.microsoft.tfs.util.Check;

/**
 * Options which control how file modification events are processed. The
 * {@link IStatus} set in the options determines whether the file modification
 * processing should continue for this modification event. If the
 * {@link IStatus} is {@link Status#OK_STATUS}, processing continues and the
 * other values specify processing details.
 *
 * @threadsafety unknown
 */
public class TFSFileModificationOptions {
    private final IStatus status;

    private final String[] files;
    private final LockLevel lockLevel;
    private final boolean isSynchronous;
    private final boolean isForeground;
    private final boolean isGetLatest;

    /**
     * Constructs a {@link TFSFileModificationOptions} with a non-OK status
     * which stops the file modification event processing.
     *
     * @param status
     *        the non-OK status (must not be <code>null</code>, status.isOK()
     *        must be <code>false</code> )
     */
    public TFSFileModificationOptions(final IStatus status) {
        Check.notNull(status, "status"); //$NON-NLS-1$
        Check.isTrue(!status.isOK(), "! status.isOK"); //$NON-NLS-1$

        this.status = status;
        files = null;
        lockLevel = null;
        isSynchronous = true;
        isForeground = true;
        isGetLatest = true;
    }

    /**
     * Constructs a {@link TFSFileModificationOptions} for an OK status.
     *
     * @param status
     *        the status (must not be <code>null</code>)
     * @param files
     *        the files to be processed (must not be <code>null</code> or empty)
     * @param lockLevel
     *        the lock level (must not be <code>null</code>)
     * @param isSynchronous
     *        pass <code>true</code> to block the application while files are
     *        checked out, <code>false</code> to not block the application
     * @param isForeground
     *        only used when isSynchronous is <code>false</code>: pass
     *        <code>true</code> to schedule the non-blocking check-out job as a
     *        foreground ("user") job, <code>false</code> for a background
     *        ("non-user") job
     * @param isGetLatest
     *        pass <code>true</code> to get the latest version of the file
     *        during check-out, <code>false</code> to check out the workspace
     *        version
     */
    public TFSFileModificationOptions(
        final IStatus status,
        final String[] files,
        final LockLevel lockLevel,
        final boolean isSynchronous,
        final boolean isForeground,
        final boolean isGetLatest) {
        Check.notNull(status, "status"); //$NON-NLS-1$
        Check.notNullOrEmpty(files, "files"); //$NON-NLS-1$
        Check.notNull(lockLevel, "lockLevel"); //$NON-NLS-1$

        this.status = status;
        this.files = files;
        this.lockLevel = lockLevel;
        this.isSynchronous = isSynchronous;
        this.isForeground = isForeground;
        this.isGetLatest = isGetLatest;
    }

    public IStatus getStatus() {
        return status;
    }

    public String[] getFiles() {
        return files;
    }

    public LockLevel getLockLevel() {
        return lockLevel;
    }

    public boolean isSynchronous() {
        return isSynchronous;
    }

    public boolean isForeground() {
        return isForeground;
    }

    public boolean isGetLatest() {
        return isGetLatest;
    }
}
