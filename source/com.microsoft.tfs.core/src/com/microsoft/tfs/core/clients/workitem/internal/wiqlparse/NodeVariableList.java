// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.wiqlparse;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public abstract class NodeVariableList extends Node {
    private final List<Node> list = new ArrayList<Node>();

    protected NodeVariableList(final NodeType nodeType) {
        super(nodeType);
    }

    public void add(final Node node) {
        list.add(node);
    }

    public void clear() {
        list.clear();
    }

    public void insert(final int ix, final Node node) {
        list.add(ix, node);
    }

    public void removeAt(final int ix) {
        list.remove(ix);
    }

    @Override
    public void appendTo(final StringBuffer b) {
        super.appendChildren(b, ", "); //$NON-NLS-1$
    }

    @Override
    public int getCount() {
        return list.size();
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
        return list.get(i);
    }

    @Override
    public void setItem(final int i, final Node value) {
        list.set(i, value);
    }

    @Override
    public Priority getPriority() {
        return null;
    }

    @Override
    public Iterator<Node> iterator() {
        return list.iterator();
    }
}
