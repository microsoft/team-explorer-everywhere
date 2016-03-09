// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teamexplorer.internal.pendingchanges;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;
import com.microsoft.tfs.util.Check;

public class PendingChangesTreeNode {
    private String subpath;
    private PendingChange pendingChange;
    private List<PendingChangesTreeNode> children;

    protected PendingChangesTreeNode(final String subpath) {
        this.subpath = subpath;
    }

    public String getSubpath() {
        return subpath;
    }

    public PendingChangesTreeNode[] getChildren() {
        if (children == null) {
            return new PendingChangesTreeNode[0];
        } else {
            return children.toArray(new PendingChangesTreeNode[children.size()]);
        }
    }

    public void addPendingChangesInSubTree(final Set<PendingChange> set) {
        addPendingChangesInSubTree(this, set);
    }

    private void addPendingChangesInSubTree(final PendingChangesTreeNode node, final Set<PendingChange> changes) {
        if (node.isLeaf()) {
            changes.add(node.getPendingChange());
            return;
        }

        for (final PendingChangesTreeNode child : node.getChildren()) {
            addPendingChangesInSubTree(child, changes);
        }
    }

    public int childCount() {
        return children == null ? 0 : children.size();
    }

    public boolean isLeaf() {
        return childCount() == 0;
    }

    public PendingChangesTreeNode getChild(final int index) {
        Check.isTrue(index < childCount(), "Index out of range"); //$NON-NLS-1$
        return children.get(index);
    }

    public void addChild(final PendingChangesTreeNode node) {
        if (children == null) {
            children = new ArrayList<PendingChangesTreeNode>();
        }

        children.add(node);
    }

    public PendingChangesTreeNode findChild(final String name) {
        if (children != null) {
            for (final PendingChangesTreeNode child : children) {
                if (child.getSubpath().equalsIgnoreCase(name)) {
                    return child;
                }
            }
        }
        return null;
    }

    public PendingChange getPendingChange() {
        return pendingChange;
    }

    public void setPendingChange(final PendingChange pendingChange) {
        this.pendingChange = pendingChange;
    }

    public void collapseRedundantChild(final String separator) {
        Check.isTrue(
            childCount() == 1 && !children.get(0).isLeaf() && children.get(0).getPendingChange() == null,
            "childCount() == 1 && !children.get(0).isLeaf() && children.get(0).getPendingChange() == null"); //$NON-NLS-1$

        final PendingChangesTreeNode node = children.get(0);
        subpath += separator;
        subpath += node.getSubpath();
        children.clear();
        children.addAll(node.children);
    }
}
