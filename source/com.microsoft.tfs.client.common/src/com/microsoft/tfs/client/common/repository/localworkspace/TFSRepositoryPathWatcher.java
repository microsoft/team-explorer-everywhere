// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.repository.localworkspace;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import com.microsoft.tfs.core.clients.versioncontrol.localworkspace.PathWatcher;
import com.microsoft.tfs.core.clients.versioncontrol.localworkspace.PathWatcherReport;
import com.microsoft.tfs.core.clients.versioncontrol.localworkspace.WorkspaceWatcher;
import com.microsoft.tfs.util.Check;

public class TFSRepositoryPathWatcher implements PathWatcher {
    private final Object lock = new Object();

    private final TFSRepositoryPathWatcherManager manager;
    private final String path;
    private final WorkspaceWatcher watcher;

    private final Set<String> changedPaths = new HashSet<String>();

    private boolean watching = false;

    public TFSRepositoryPathWatcher(
        final TFSRepositoryPathWatcherManager manager,
        final String path,
        final WorkspaceWatcher watcher) {
        Check.notNull(manager, "manager"); //$NON-NLS-1$
        Check.notNull(path, "path"); //$NON-NLS-1$
        Check.notNull(watcher, "watcher"); //$NON-NLS-1$

        this.manager = manager;
        this.path = path;
        this.watcher = watcher;
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public PathWatcherReport poll() {
        final PathWatcherReport report = new PathWatcherReport(false);

        synchronized (lock) {
            for (final String changedPath : changedPaths) {
                report.addChangedPath(changedPath);
            }

            changedPaths.clear();
        }

        return report;
    }

    public WorkspaceWatcher getWatcher() {
        return watcher;
    }

    @Override
    public void startWatching() {
        synchronized (lock) {
            if (watching) {
                return;
            }

            manager.addPathWatcher(path, this);
            watching = true;
        }
    }

    @Override
    public void stopWatching() {
        synchronized (lock) {
            if (!watching) {
                return;
            }

            manager.removePathWatcher(path, this);
            watching = false;
        }
    }

    @Override
    public boolean isWatching() {
        synchronized (lock) {
            return watching;
        }
    }

    @Override
    public boolean hasChanged() {
        synchronized (lock) {
            return changedPaths.size() != 0;
        }
    }

    @Override
    public void setClean() {
        synchronized (lock) {
            changedPaths.clear();
        }
    }

    public void notifyPathChanged(final String path) {
        synchronized (lock) {
            changedPaths.add(path);
        }

        watcher.pathChanged(this);
    }

    public void notifyPathsChanged(final Collection<String> paths) {
        synchronized (lock) {
            for (final String path : paths) {
                changedPaths.add(path);
            }
        }

        watcher.pathChanged(this);
    }
}
