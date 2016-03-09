// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teamexplorer.sections;

import org.eclipse.jface.viewers.TreeViewer;

/**
 *
 * @threadsafety unknown
 */
public class TeamExplorerWorkItemsQueriesState {

    private Object[] expandedElements;

    /**
     *
     * @param expandedElements
     */
    public TeamExplorerWorkItemsQueriesState(final Object[] expandedElements) {
        this.expandedElements = expandedElements;
    }

    public TeamExplorerWorkItemsQueriesState(final TreeViewer treeViewer) {
        if (treeViewer == null) {
            this.expandedElements = null;
        } else {
            this.expandedElements = treeViewer.getExpandedElements();
        }
    }

    /**
     * @return
     */
    public Object[] getExpandedElements() {
        return expandedElements;
    }

    /**
     * @param expandedElements
     */
    public void setExpandedElements(final Object[] expandedElements) {
        this.expandedElements = expandedElements;
    }

    public void updateTreeState(final TreeViewer treeViewer) {
        if (treeViewer == null) {
            this.expandedElements = null;
        } else {
            this.expandedElements = treeViewer.getExpandedElements();
        }
    }

    public void restoreState(final TreeViewer treeViewer) {
        if (treeViewer == null) {
            return;
        }
        if (expandedElements == null) {
            treeViewer.expandAll();
        } else {
            treeViewer.setExpandedElements(expandedElements);
        }
    }
}
