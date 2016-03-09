// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.localworkspace;

/**
 * An interface that allows more optimized local path access, by watching files
 * for changes and only scanning those as necessary.
 *
 * @threadsafety thread-safe
 */
public interface PathWatcher {
    /**
     * Gets the root path that this watcher is watching
     *
     * @return The path this watcher is watching (not <code>null</code>)
     */
    public String getPath();

    /**
     * Queries if there are any paths that have changed since the watching
     * started (or since the last call to {@link #setClean()}.
     *
     * @return <code>true</code> if paths have changed, <code>false</code>
     *         otherwise
     */
    public boolean hasChanged();

    /**
     * Enables this path watcher.
     */
    public void startWatching();

    /**
     * Disables this path watcher.
     */
    public void stopWatching();

    /**
     * Queries whether this path watcher is currently watching.
     *
     * @return <code>true</code> if this watcher is operating,
     *         <code>false</code> otherwise
     */
    public boolean isWatching();

    /**
     * Sets this watcher to clean, removing any paths that have been changed.
     */
    public void setClean();

    /**
     * Retrieves a report of the paths which have changed under this
     * PathWatcher's observations since the last time Poll() was invoked.
     *
     *
     * @return A PathWatcherReport
     */
    public PathWatcherReport poll();
}
