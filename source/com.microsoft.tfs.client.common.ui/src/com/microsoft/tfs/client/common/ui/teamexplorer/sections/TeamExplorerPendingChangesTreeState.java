// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teamexplorer.sections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.viewers.TreeViewer;

import com.microsoft.tfs.client.common.ui.teamexplorer.internal.pendingchanges.PendingChangesTree;
import com.microsoft.tfs.client.common.ui.teamexplorer.internal.pendingchanges.PendingChangesTreeNode;

/**
 *
 * @threadsafety unknown
 */
public class TeamExplorerPendingChangesTreeState {

    private Set<String> collapsePaths;

    /**
     *
     * @param collapsePaths
     */
    public TeamExplorerPendingChangesTreeState(final TreeViewer treeViewer) {
        this.collapsePaths = getCollapsedSubpaths(treeViewer);
    }

    /**
     *
     * @param collapsePaths
     */
    public TeamExplorerPendingChangesTreeState(final Set<String> collapsePaths) {
        this.collapsePaths = collapsePaths;
    }

    /**
     * @return
     */
    public Set<String> getCollapsePaths() {
        return collapsePaths;
    }

    /**
     * @param collapsePaths
     */
    public void setCollapsePaths(final Set<String> collapsePaths) {
        this.collapsePaths = collapsePaths;
    }

    public void updateTreeState(final TreeViewer treeViewer) {
        this.collapsePaths = getCollapsedSubpaths(treeViewer);
    }

    public void restoreTreeState(final TreeViewer treeViewer, final PendingChangesTree tree) {
        if (this.collapsePaths == null) {
            treeViewer.expandAll();
        } else {
            treeViewer.setExpandedElements(getExpandedNodes(tree, collapsePaths));
        }
    }

    private Object[] getExpandedNodes(final PendingChangesTree tree, final Set<String> pathSet) {
        final List<PendingChangesTreeNode> expandedNodes = new ArrayList<PendingChangesTreeNode>();
        final LinkedList<PendingChangesTreeNode> nodeQueue = new LinkedList<PendingChangesTreeNode>();

        nodeQueue.addAll(Arrays.asList(tree.getRoots()));
        while (nodeQueue.size() > 0) {
            final PendingChangesTreeNode n = nodeQueue.poll();
            if (!pathSet.contains(n.getSubpath())) {
                expandedNodes.add(n);
                final PendingChangesTreeNode[] childNodes = n.getChildren();
                if (childNodes.length > 0) {
                    nodeQueue.addAll(Arrays.asList(childNodes));
                }
            }
        }

        return expandedNodes.toArray(new PendingChangesTreeNode[expandedNodes.size()]);
    }

    private Set<String> getCollapsedSubpaths(final TreeViewer treeViewer) {
        if (treeViewer == null) {
            return null;
        }
        final PendingChangesTree tree = (PendingChangesTree) treeViewer.getInput();
        if (tree == null || tree.getRoots() == null) {
            return null;
        }

        final Set<String> nodeSet = new HashSet<String>();
        final LinkedList<PendingChangesTreeNode> nodeQueue = new LinkedList<PendingChangesTreeNode>();
        nodeQueue.addAll(Arrays.asList(tree.getRoots()));
        while (nodeQueue.size() > 0) {
            final PendingChangesTreeNode n = nodeQueue.poll();
            nodeSet.add(n.getSubpath());
            final PendingChangesTreeNode[] childNodes = n.getChildren();
            if (childNodes.length > 0) {
                nodeQueue.addAll(Arrays.asList(childNodes));
            }
        }

        final Object[] elements = treeViewer.getExpandedElements();
        for (final Object o : elements) {
            if (o instanceof PendingChangesTreeNode) {
                final PendingChangesTreeNode node = (PendingChangesTreeNode) o;
                nodeSet.remove(node.getSubpath());
            }
        }
        return nodeSet;
    }
}
