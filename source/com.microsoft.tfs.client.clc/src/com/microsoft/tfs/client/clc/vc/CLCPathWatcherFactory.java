// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.clc.vc;

import com.microsoft.tfs.core.clients.versioncontrol.localworkspace.PathWatcher;
import com.microsoft.tfs.core.clients.versioncontrol.localworkspace.PathWatcherFactory;
import com.microsoft.tfs.core.clients.versioncontrol.localworkspace.WorkspaceWatcher;

public class CLCPathWatcherFactory implements PathWatcherFactory {
    @Override
    public PathWatcher newPathWatcher(final String path, final WorkspaceWatcher watcher) {
        return new CLCPathWatcher(path, watcher);
    }
}
