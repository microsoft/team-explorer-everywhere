// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.wiqlparse;

import java.util.Locale;

public class NodeString extends NodeItem {
    public NodeString(final String value) {
        super(NodeType.STRING, value);
    }

    @Override
    public void appendTo(final StringBuffer b) {
        Tools.AppendString(b, getValue());
    }

    @Override
    public String getConstStringValue() {
        return getValue();
    }

    @Override
    public DataType getDataType() {
        return DataType.STRING;
    }

    @Override
    public boolean isConst() {
        return true;
    }

    @Override
    public boolean canCastTo(final DataType dataType, final Locale locale) {
        if (dataType == DataType.NUMERIC) {
            if (getValue().length() != 0) {
                return Tools.isNumericString(getValue());
            }
            return true;
        } else if (dataType == DataType.DATE) {
            if (getValue().length() != 0) {
                return Tools.isDateString(getValue(), locale);
            }
            return true;
        } else if (dataType == DataType.STRING) {
            return true;
        } else if (dataType == DataType.GUID) {
            return ((getValue().length() == 0) || Tools.IsGUIDString(getValue()));
        }

        else {
            return false;
        }
    }
}
