// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.wiqlparse;

public class NodeBoolConst extends Node {
    private final boolean boolConst;

    public NodeBoolConst(final boolean b) {
        super(NodeType.BOOL_CONST);
        boolConst = b;
    }

    @Override
    public void appendTo(final StringBuffer b) {
        b.append(boolConst);
    }

    @Override
    public int getCount() {
        return 0;
    }

    @Override
    public DataType getDataType() {
        return DataType.BOOL;
    }

    @Override
    public boolean isConst() {
        return true;
    }

    @Override
    public boolean isScalar() {
        return false;
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

    public boolean getValue() {
        return boolConst;
    }
}
