// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.clc.vc;

import com.microsoft.tfs.core.clients.versioncontrol.localworkspace.PathWatcher;
import com.microsoft.tfs.core.clients.versioncontrol.localworkspace.PathWatcherReport;
import com.microsoft.tfs.core.clients.versioncontrol.localworkspace.WorkspaceWatcher;
import com.microsoft.tfs.util.Check;

/**
 * A {@link PathWatcher} for the CLC that doesn't actually connect to the
 * operating system's path watchers, but exists to make core do at most one full
 * scan when executing a CLC command.
 *
 * @threadsafety thread-safe
 */
public class CLCPathWatcher implements PathWatcher {
    private final String path;

    private volatile boolean watching = false;

    public CLCPathWatcher(final String path, final WorkspaceWatcher localWorkspaceWatcher) {
        Check.notNull(path, "path"); //$NON-NLS-1$
        Check.notNull(localWorkspaceWatcher, "localWorkspaceWatcher"); //$NON-NLS-1$

        this.path = path;
    }

    @Override
    public PathWatcherReport poll() {
        return new PathWatcherReport(false);
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public boolean isWatching() {
        return watching;
    }

    @Override
    public boolean hasChanged() {
        return false;
    }

    @Override
    public void startWatching() {
        watching = true;
    }

    @Override
    public void stopWatching() {
        watching = false;
    }

    @Override
    public void setClean() {
        // hasChanged will always return false; nothing to do
    }
}
