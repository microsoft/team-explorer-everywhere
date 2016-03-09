// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.workspacecache;

import com.microsoft.tfs.util.TypesafeEnum;

public class LocalWorkspaceState extends TypesafeEnum {
    // The values are important. The state value for a workspace object must
    // never decrease.

    /**
     * Came from the cache.
     */
    public static final LocalWorkspaceState CLEAN = new LocalWorkspaceState(0);

    /**
     * Came from the cache and has been changed.
     */
    public static final LocalWorkspaceState MODIFIED = new LocalWorkspaceState(1);

    /**
     * Came from the cache and has been removed.
     */
    public static final LocalWorkspaceState REMOVED = new LocalWorkspaceState(2);

    /**
     * New workspace.
     */
    public static final LocalWorkspaceState NEW = new LocalWorkspaceState(3);

    private LocalWorkspaceState(final int value) {
        super(value);
    }
}