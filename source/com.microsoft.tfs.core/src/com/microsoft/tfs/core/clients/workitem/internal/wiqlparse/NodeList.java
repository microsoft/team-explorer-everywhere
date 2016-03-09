// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.wiqlparse;

public abstract class NodeList extends Node {
    private final Node[] nodeArray;

    protected NodeList(final NodeType nodeType, final int count) {
        super(nodeType);
        nodeArray = new Node[count];
    }

    @Override
    public void appendTo(final StringBuffer b) {
        super.appendChildren(b, ", "); //$NON-NLS-1$
    }

    @Override
    public int getCount() {
        return nodeArray.length;
    }

    @Override
    public DataType getDataType() {
        return getChildrenDataType();
    }

    @Override
    public boolean isConst() {
        return isChildrenConst();
    }

    @Override
    public boolean isScalar() {
        return false;
    }

    @Override
    public Node getItem(final int i) {
        return nodeArray[i];
    }

    @Override
    public void setItem(final int i, final Node value) {
        nodeArray[i] = value;
    }
}
