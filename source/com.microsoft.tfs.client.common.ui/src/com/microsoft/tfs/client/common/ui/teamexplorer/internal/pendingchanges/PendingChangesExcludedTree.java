// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teamexplorer.internal.pendingchanges;

public class PendingChangesExcludedTree extends PendingChangesTree {
    @Override
    public PendingChangesTreeNode createTreeNode(final String subpath) {
        return new PendingChangesExcludedTreeNode(subpath);
    }
}
