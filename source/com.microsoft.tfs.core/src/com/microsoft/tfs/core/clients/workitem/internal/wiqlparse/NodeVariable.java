// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.wiqlparse;

public class NodeVariable extends NodeItem {
    private DataType dataType;
    private Object tag;

    public NodeVariable(final String value) {
        super(NodeType.VARIABLE, value);
    }

    @Override
    public void bind(final IExternal e, final NodeTableName tableContext, final NodeFieldName fieldContext) {
        if (e != null) {
            tag = e.findVariable(getValue());
            Tools.ensureSyntax(tag != null, SyntaxError.VARIABLE_DOES_NOT_EXIST, this);
            dataType = e.getVariableDataType(tag);
            Tools.ensureSyntax(dataType != DataType.UNKNOWN, SyntaxError.UNKNOWN_VARIABLE_TYPE, this);
        }
        super.bind(e, tableContext, fieldContext);
    }

    @Override
    public void appendTo(final StringBuffer b) {
        b.append("@" + getValue()); //$NON-NLS-1$
    }

    @Override
    public DataType getDataType() {
        return dataType;
    }

    @Override
    public boolean isConst() {
        return true;
    }

    public Object getTag() {
        return tag;
    }
}
