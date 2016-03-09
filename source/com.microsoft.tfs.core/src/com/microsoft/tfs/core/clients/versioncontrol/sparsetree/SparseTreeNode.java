// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.sparsetree;

import java.util.List;

public class SparseTreeNode<X> {
    public String token;
    public String[] tokenElements;
    public X referencedObject;
    public SparseTreeNode<X> parent;

    // Child list will be created by the AddNode method when necessary.
    public List<SparseTreeNode<X>> children;

    /**
     * Returns a new SparseTreeNode
     */
    public SparseTreeNode(final String token, final String[] tokenElements, final X referencedObject) {
        this.token = token;
        this.tokenElements = tokenElements;
        this.referencedObject = referencedObject;
        // Child list will be created by the AddNode method when necessary.
        children = null;
    }

    /**
     * Returns a new SparseTreeNode
     */
    public SparseTreeNode(
        final String token,
        final String[] tokenElements,
        final X referencedObject,
        final List<SparseTreeNode<X>> children) {
        this(token, tokenElements, referencedObject);
        this.children = children;
    }

    /**
     * The number of children of this SparseTreeNode
     */
    public int getChildCount() {
        if (null != children) {
            return children.size();
        }

        return 0;
    }
}
