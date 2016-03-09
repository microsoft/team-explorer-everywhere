// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.wiqlparse;

import java.util.Locale;

public class NodeNumber extends NodeItem {
    public NodeNumber(final String value) {
        super(NodeType.NUMBER, value);
    }

    @Override
    public void bind(final IExternal e, final NodeTableName tableContext, final NodeFieldName fieldContext) {
        Tools.ensureSyntax(Tools.isNumericString(getValue()), SyntaxError.STRING_IS_NOT_A_NUMBER, this);
        super.bind(e, tableContext, fieldContext);
    }

    @Override
    public boolean canCastTo(final DataType type, final Locale locale) {
        if (!type.equals(DataType.BOOL)) {
            return super.canCastTo(type, locale);
        }
        return true;
    }

    @Override
    public DataType getDataType() {
        return DataType.NUMERIC;
    }

    @Override
    public boolean isConst() {
        return true;
    }

    @Override
    public String getConstStringValue() {
        return getValue();
    }
}
