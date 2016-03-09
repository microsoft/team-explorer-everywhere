// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teamexplorer.internal.pendingchanges;

import com.microsoft.tfs.client.common.ui.framework.tree.TreeContentProvider;
import com.microsoft.tfs.util.Check;

public class PendingChangesTreeContentProvider extends TreeContentProvider {
    @Override
    public Object[] getElements(final Object inputElement) {
        Check.isTrue(inputElement instanceof PendingChangesTree, "inputElement instanceof PendingChangesTree"); //$NON-NLS-1$
        return ((PendingChangesTree) inputElement).getRoots();
    }

    @Override
    public Object[] getChildren(final Object parentElement) {
        Check.isTrue(
            parentElement instanceof PendingChangesTreeNode,
            "parentElement instanceof PendingChangesTreeNode"); //$NON-NLS-1$
        return ((PendingChangesTreeNode) parentElement).getChildren();
    }

    @Override
    public boolean hasChildren(final Object element) {
        Check.isTrue(element instanceof PendingChangesTreeNode, "element instanceof PendingChangesTreeNode"); //$NON-NLS-1$
        return ((PendingChangesTreeNode) element).childCount() > 0;
    }
}
