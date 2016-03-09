// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.internal.localworkspace;

import com.microsoft.tfs.core.clients.versioncontrol.localworkspace.PathWatcher;
import com.microsoft.tfs.core.clients.versioncontrol.localworkspace.PathWatcherReport;
import com.microsoft.tfs.core.clients.versioncontrol.localworkspace.WorkspaceWatcher;
import com.microsoft.tfs.util.Check;

/**
 * A simple implementation of {@link PathWatcher} that is a noop. This does not
 * do any background path watching, and simply reports itself as disabled,
 * causing a full scan to be required at all times.
 *
 * @threadsafety unknown
 */
public class NullPathWatcher implements PathWatcher {
    private final String workspaceRoot;

    public NullPathWatcher(final String workspaceRoot, final WorkspaceWatcher localWorkspaceWatcher) {
        Check.notNull(workspaceRoot, "workspaceRoot"); //$NON-NLS-1$
        Check.notNull(localWorkspaceWatcher, "localWorkspaceWatcher"); //$NON-NLS-1$

        this.workspaceRoot = workspaceRoot;
    }

    @Override
    public String getPath() {
        return workspaceRoot;
    }

    @Override
    public boolean isWatching() {
        return false;
    }

    @Override
    public boolean hasChanged() {
        return false;
    }

    @Override
    public void startWatching() {
    }

    @Override
    public void stopWatching() {
    }

    @Override
    public void setClean() {
    }

    public String[] getChangedPaths() {
        return new String[0];
    }

    @Override
    public PathWatcherReport poll() {
        return new PathWatcherReport(false);
    }
}
