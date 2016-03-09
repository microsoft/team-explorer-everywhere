// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.repository.localworkspace;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.core.clients.versioncontrol.localworkspace.PathWatcher;
import com.microsoft.tfs.core.clients.versioncontrol.localworkspace.PathWatcherFactory;
import com.microsoft.tfs.core.clients.versioncontrol.localworkspace.WorkspaceWatcher;
import com.microsoft.tfs.core.clients.versioncontrol.path.LocalPath;
import com.microsoft.tfs.util.Check;

public class TFSRepositoryPathWatcherManager implements PathWatcherFactory {
    private final Map<String, Set<TFSRepositoryPathWatcher>> watcherMap =
        new HashMap<String, Set<TFSRepositoryPathWatcher>>();

    public TFSRepositoryPathWatcherManager(final TFSRepository repository) {
    }

    @Override
    public PathWatcher newPathWatcher(final String workspaceRoot, final WorkspaceWatcher watcher) {
        return new TFSRepositoryPathWatcher(this, workspaceRoot, watcher);
    }

    public void addPathWatcher(final String path, final TFSRepositoryPathWatcher watcher) {
        Check.notNull(path, "path"); //$NON-NLS-1$
        Check.notNull(watcher, "watcher"); //$NON-NLS-1$

        synchronized (watcherMap) {
            Set<TFSRepositoryPathWatcher> watchersForPath = watcherMap.get(path);

            if (watchersForPath == null) {
                watchersForPath = new HashSet<TFSRepositoryPathWatcher>();
                watcherMap.put(path, watchersForPath);
            }

            watchersForPath.add(watcher);
        }
    }

    public TFSRepositoryPathWatcher[] getPathWatchers() {
        final Set<TFSRepositoryPathWatcher> pathWatchers = new HashSet<TFSRepositoryPathWatcher>();

        synchronized (watcherMap) {
            for (final Entry<String, Set<TFSRepositoryPathWatcher>> entry : watcherMap.entrySet()) {
                pathWatchers.addAll(entry.getValue());
            }
        }

        return pathWatchers.toArray(new TFSRepositoryPathWatcher[pathWatchers.size()]);
    }

    public TFSRepositoryPathWatcher[] getPathWatchers(String path) {
        Check.notNull(path, "path"); //$NON-NLS-1$

        synchronized (watcherMap) {
            do {
                final Set<TFSRepositoryPathWatcher> pathWatchers = watcherMap.get(path);

                if (pathWatchers != null) {
                    return pathWatchers.toArray(new TFSRepositoryPathWatcher[pathWatchers.size()]);
                }

                path = LocalPath.getParent(path);
            } while (path != null && !path.equals(LocalPath.getPathRoot(path)));
        }

        return new TFSRepositoryPathWatcher[0];
    }

    public void removePathWatcher(final String path, final TFSRepositoryPathWatcher watcher) {
        Check.notNull(path, "path"); //$NON-NLS-1$
        Check.notNull(watcher, "watcher"); //$NON-NLS-1$

        synchronized (watcherMap) {
            final Set<TFSRepositoryPathWatcher> watchersForPath = watcherMap.get(path);

            if (watchersForPath != null) {
                watchersForPath.remove(watcher);
            }
        }
    }

    /**
     * Notifies the scanner to perform a full refresh (by notifying each path
     * watcher that their root path has changed.)
     */
    public void forceFullScan() throws IOException {
        final Set<WorkspaceWatcher> watchers = new HashSet<WorkspaceWatcher>();

        for (final TFSRepositoryPathWatcher watcher : getPathWatchers()) {
            watchers.add(watcher.getWatcher());
        }

        for (final WorkspaceWatcher watcher : watchers) {
            watcher.forceFullScan();
        }
    }

    /**
     * Notifies the scanner that the given path has changed.
     *
     * @param path
     *        The local path that changed
     */
    public void notifyWatchers(final String path) {
        Check.notNull(path, "path"); //$NON-NLS-1$

        final TFSRepositoryPathWatcher[] watchers = getPathWatchers(path);

        for (final TFSRepositoryPathWatcher watcher : watchers) {
            watcher.notifyPathChanged(path);
        }
    }

    /**
     * Notifies the scanner that the given paths have changed. This may be
     * slightly more efficient than calling multiple
     * {@link #notifyWatchers(String)}.
     */
    public void notifyWatchers(final Collection<String> paths) {
        Check.notNull(paths, "paths"); //$NON-NLS-1$

        final Map<TFSRepositoryPathWatcher, Set<String>> pathsToPathWatcher =
            new HashMap<TFSRepositoryPathWatcher, Set<String>>();

        for (final String path : paths) {
            final TFSRepositoryPathWatcher[] watchers = getPathWatchers(path);

            for (final TFSRepositoryPathWatcher watcher : watchers) {
                Set<String> pathsForThisWatcher = pathsToPathWatcher.get(watcher);

                if (pathsForThisWatcher == null) {
                    pathsForThisWatcher = new HashSet<String>();
                    pathsToPathWatcher.put(watcher, pathsForThisWatcher);
                }

                pathsForThisWatcher.add(path);
            }
        }

        for (final Entry<TFSRepositoryPathWatcher, Set<String>> entry : pathsToPathWatcher.entrySet()) {
            final TFSRepositoryPathWatcher watcher = entry.getKey();
            final Set<String> pathsForWatcher = entry.getValue();

            watcher.notifyPathsChanged(pathsForWatcher);
        }
    }
}
