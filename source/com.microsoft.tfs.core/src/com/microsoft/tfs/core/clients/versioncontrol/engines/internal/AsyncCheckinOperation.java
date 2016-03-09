// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.engines.internal;

import com.microsoft.tfs.core.clients.versioncontrol.internal.localworkspace.BaselineFolderCollection;
import com.microsoft.tfs.core.clients.versioncontrol.internal.localworkspace.WorkspaceLock;

/**
 * Contains state information used by the {@link CheckinEngine} class to track
 * its progress and errors.
 *
 * @threadsafety thread-safe
 */
public final class AsyncCheckinOperation extends AsyncOperation {
    private final WorkspaceLock workspaceLock;

    public AsyncCheckinOperation(final WorkspaceLock wLock) {
        super();

        this.workspaceLock = wLock;
    }

    public WorkspaceLock getWorkspaceLock() {
        return workspaceLock;
    }

    public BaselineFolderCollection getBaselineFolders() {
        if (null == workspaceLock) {
            return null;
        }

        return workspaceLock.getBaselineFolders();
    }
}
