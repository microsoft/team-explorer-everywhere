// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.wiqlparse;

public class NodeArithmetic extends NodeList {
    private Arithmetic operator;

    public NodeArithmetic() {
        super(NodeType.ARITHMETIC, 2);
    }

    @Override
    public void appendTo(final StringBuffer b) {
        super.appendChildren(b, " " + ArithmeticalOperators.getString(operator) + " "); //$NON-NLS-1$ //$NON-NLS-2$
    }

    @Override
    public void bind(final IExternal e, final NodeTableName tableContext, final NodeFieldName fieldContext) {
        getLeft().bind(e, tableContext, fieldContext);
        getRight().bind(e, tableContext, fieldContext);
        Tools.ensureSyntax(getLeft().isConst(), SyntaxError.EXPECTING_CONST, getLeft());
        Tools.ensureSyntax(getRight().isConst(), SyntaxError.EXPECTING_CONST, getRight());
        super.bind(e, tableContext, fieldContext);
    }

    @Override
    public Node optimize(final IExternal e, final NodeTableName tableContext, final NodeFieldName fieldContext) {
        optimizeChildren(e, tableContext, fieldContext);
        return super.optimize(e, tableContext, fieldContext);
    }

    public Arithmetic getArithmetic() {
        return operator;
    }

    public void setArithmetic(final Arithmetic operator) {
        this.operator = operator;
    }

    @Override
    public DataType getDataType() {
        return getLeft().getDataType();
    }

    public Node getLeft() {
        return getItem(0);
    }

    public void setLeft(final Node left) {
        setItem(0, left);
    }

    @Override
    public Priority getPriority() {
        return Priority.ADD_OPERATOR;
    }

    public Node getRight() {
        return getItem(1);
    }

    public void setRight(final Node right) {
        setItem(1, right);
    }
}
