// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.localworkspace;

/**
 * A factory for {@link PathWatcher}s.
 *
 * @threadsafety thread-safe
 */
public interface PathWatcherFactory {
    /**
     * Gets a new {@link PathWatcher} that watches for filesystem changes at the
     * specified directory and in all subdirectories.
     *
     * @param path
     *        the local directory path to watch for changes (must not be
     *        <code>null</code>)
     * @param watcher
     *        the {@link WorkspaceWatcher} that the new {@link PathWatcher} will
     *        work for. The {@link PathWatcher} may notify the
     *        {@link LocalWorkspaceScanner} directly of changes it detects.
     * @return a new {@link PathWatcher} (never <code>null</code>)
     */
    PathWatcher newPathWatcher(String path, WorkspaceWatcher watcher);
}
