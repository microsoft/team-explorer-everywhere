// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.tree;

import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;

/**
 * Implements the IDoubleClickListener interface to provide functionality to
 * TreeViewers that expands/collapses node on double clicking.
 */
public class TreeViewerDoubleClickListener implements IDoubleClickListener {
    private final TreeViewer treeViewer;

    public TreeViewerDoubleClickListener(final TreeViewer treeViewer) {
        this.treeViewer = treeViewer;
    }

    @Override
    public void doubleClick(final DoubleClickEvent event) {
        final IStructuredSelection selection = (IStructuredSelection) event.getSelection();
        final Object element = selection.getFirstElement();
        final boolean expanded = treeViewer.getExpandedState(element);
        treeViewer.setExpandedState(element, !expanded);
    }
}
