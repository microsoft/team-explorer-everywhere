// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.wiqlparse;

public class NodeBoolValue extends NodeItem {

    private final boolean bool;

    public NodeBoolValue(final boolean b) {
        super(NodeType.BOOL_VALUE, Boolean.toString(b));
        bool = b;
    }

    @Override
    public DataType getDataType() {
        return DataType.BOOL;
    }

    public boolean getBoolValue() {
        return bool;
    }

    @Override
    public boolean isConst() {
        return true;
    }

    @Override
    public void bind(final IExternal e, final NodeTableName tableContext, final NodeFieldName fieldContext) {
        final Boolean flag = Tools.TranslateBoolToken(getValue());
        Tools.ensureSyntax((flag != null), SyntaxError.EXPECTING_BOOLEAN, this);
        super.bind(e, tableContext, fieldContext);
    }

    @Override
    public void appendTo(final StringBuffer b) {
        final String valueString = bool ? "True" : "False"; //$NON-NLS-1$ //$NON-NLS-2$
        b.append(valueString);
    }
}
