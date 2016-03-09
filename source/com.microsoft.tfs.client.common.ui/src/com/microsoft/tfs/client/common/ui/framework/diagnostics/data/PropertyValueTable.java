// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.diagnostics.data;

import java.util.Enumeration;
import java.util.Locale;
import java.util.Properties;

import com.microsoft.tfs.client.common.ui.Messages;

public class PropertyValueTable extends TabularData {
    public PropertyValueTable(final Locale locale) {
        super(new String[] {
            Messages.getString("PropertyValueTable.ColumNameProperty", locale), //$NON-NLS-1$
            Messages.getString("PropertyValueTable.ColumnNameValue", locale) //$NON-NLS-1$
        });
    }

    public void addProperty(final String key, final Object value) {
        addRow(new Row(new Object[] {
            key,
            value
        }));
    }

    public void addAll(final Properties properties) {
        final Enumeration names = properties.propertyNames();
        while (names.hasMoreElements()) {
            final String name = (String) names.nextElement();
            final String value = properties.getProperty(name);
            addProperty(name, value);
        }
    }
}
