// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.wiqlparse;

public class NodeTableName extends NodeItem {
    private Object tag;

    public NodeTableName(final NodeName nodeName) {
        super(NodeType.TABLE_NAME, nodeName.getValue());
        setStartOffset(nodeName.getStartOffset());
        setEndOffset(nodeName.getEndOffset());
    }

    public NodeTableName(final String s) {
        super(NodeType.TABLE_NAME, s);
    }

    @Override
    public void bind(final IExternal e, final NodeTableName tableContext, final NodeFieldName fieldContext) {
        if (e != null) {
            tag = e.findTable(getValue());
            Tools.ensureSyntax(tag != null, SyntaxError.TABLE_DOES_NOT_EXIST, this);
        }
        super.bind(e, tableContext, fieldContext);
    }

    @Override
    public void appendTo(final StringBuffer b) {
        Tools.AppendName(b, getValue());
    }

    @Override
    public DataType getDataType() {
        return DataType.TABLE;
    }

    @Override
    public boolean isConst() {
        return true;
    }

    public Object getTag() {
        return tag;
    }

    public void setTag(final Object obj) {
        tag = obj;
    }
}
