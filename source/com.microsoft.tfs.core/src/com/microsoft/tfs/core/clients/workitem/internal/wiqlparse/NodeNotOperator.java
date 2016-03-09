// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.wiqlparse;

public class NodeNotOperator extends Node {
    private Node child;

    public NodeNotOperator() {
        super(NodeType.NOT);
    }

    public NodeNotOperator(final Node node) {
        super(NodeType.NOT);
        child = node;
    }

    @Override
    public void bind(final IExternal e, final NodeTableName tableContext, final NodeFieldName fieldContext) {
        getValue().bind(e, tableContext, fieldContext);
        Tools.ensureSyntax(getValue().getDataType() == DataType.BOOL, SyntaxError.EXPECTING_BOOLEAN, this);
        super.bind(e, tableContext, fieldContext);
    }

    @Override
    public Node optimize(final IExternal e, final NodeTableName tableContext, final NodeFieldName fieldContext) {
        setValue(getValue().optimize(e, tableContext, fieldContext));
        if (getValue().getNodeType() == NodeType.BOOL_CONST) {
            return new NodeBoolConst(!((NodeBoolConst) getValue()).getValue());
        }
        if (getValue().getNodeType() == NodeType.NOT) {
            return ((NodeNotOperator) getValue()).getValue();
        }
        return super.optimize(e, tableContext, fieldContext);
    }

    @Override
    public void appendTo(final StringBuffer b) {
        b.append("not "); //$NON-NLS-1$
        super.appendChildren(b, ""); //$NON-NLS-1$
    }

    @Override
    public int getCount() {
        return 1;
    }

    @Override
    public DataType getDataType() {
        return DataType.BOOL;
    }

    @Override
    public boolean isConst() {
        return child.isConst();
    }

    @Override
    public boolean isScalar() {
        return false;
    }

    @Override
    public Node getItem(final int i) {
        return child;
    }

    @Override
    public void setItem(final int i, final Node value) {
        child = value;
    }

    @Override
    public Priority getPriority() {
        return Priority.UNARY_BOOL_OPERATOR;
    }

    public Node getValue() {
        return child;
    }

    public void setValue(final Node value) {
        child = value;
    }
}
