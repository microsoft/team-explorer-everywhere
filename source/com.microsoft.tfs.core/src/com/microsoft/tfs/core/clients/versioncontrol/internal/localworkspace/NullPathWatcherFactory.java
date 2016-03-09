// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.internal.localworkspace;

import com.microsoft.tfs.core.clients.versioncontrol.localworkspace.PathWatcher;
import com.microsoft.tfs.core.clients.versioncontrol.localworkspace.PathWatcherFactory;
import com.microsoft.tfs.core.clients.versioncontrol.localworkspace.WorkspaceWatcher;

public class NullPathWatcherFactory implements PathWatcherFactory {
    @Override
    public PathWatcher newPathWatcher(final String workspaceRoot, final WorkspaceWatcher watcher) {
        return new NullPathWatcher(workspaceRoot, watcher);
    }
}
