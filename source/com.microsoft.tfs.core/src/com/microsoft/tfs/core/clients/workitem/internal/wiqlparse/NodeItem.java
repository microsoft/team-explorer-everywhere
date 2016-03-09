// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.wiqlparse;

public abstract class NodeItem extends Node {
    private String value;

    protected NodeItem(final NodeType nodeType, final String value) {
        super(nodeType);
        this.value = value;
    }

    @Override
    public void appendTo(final StringBuffer b) {
        b.append(value);
    }

    @Override
    public int getCount() {
        return 0;
    }

    @Override
    public boolean isScalar() {
        return true;
    }

    @Override
    public Node getItem(final int i) {
        return null;
    }

    @Override
    public void setItem(final int i, final Node value) {
    }

    @Override
    public Priority getPriority() {
        return Priority.OPERAND;
    }

    public String getValue() {
        return value;
    }

    public void setValue(final String value) {
        this.value = value;
    }
}
