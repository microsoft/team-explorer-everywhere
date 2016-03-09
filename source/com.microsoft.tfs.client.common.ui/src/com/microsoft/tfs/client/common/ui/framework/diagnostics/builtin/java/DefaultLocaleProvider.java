// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.diagnostics.builtin.java;

import java.util.Locale;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.framework.diagnostics.DiagnosticLocale;
import com.microsoft.tfs.client.common.ui.framework.diagnostics.data.Row;
import com.microsoft.tfs.client.common.ui.framework.diagnostics.data.TabularData;
import com.microsoft.tfs.client.common.ui.framework.diagnostics.extend.DataProvider;

public class DefaultLocaleProvider implements DataProvider {
    @Override
    public Object getData() {
        return getData(DiagnosticLocale.USER_LOCALE);
    }

    @Override
    public Object getDataNOLOC() {
        return getData(DiagnosticLocale.SUPPORT_LOCALE);
    }

    private Object getData(final Locale displayLocale) {
        final TabularData table = new TabularData(new String[] {
            Messages.getString("DefaultLocaleProvider.ColumnNameLocale", displayLocale), //$NON-NLS-1$
            Messages.getString("DefaultLocaleProvider.ColumnNameValue", displayLocale), //$NON-NLS-1$
            Messages.getString("DefaultLocaleProvider.ColumnNameDisplayName", displayLocale) //$NON-NLS-1$
        });

        final Locale defaultLocale = Locale.getDefault();

        table.addRow(new Row(new String[] {
            Messages.getString("DefaultLocaleProvider.Country", displayLocale), //$NON-NLS-1$
            defaultLocale.getCountry(),
            defaultLocale.getDisplayCountry(displayLocale)
        }));

        table.addRow(new Row(new String[] {
            Messages.getString("DefaultLocaleProvider.Language", displayLocale), //$NON-NLS-1$
            defaultLocale.getLanguage(),
            defaultLocale.getDisplayLanguage(displayLocale)
        }));

        table.addRow(new Row(new String[] {
            Messages.getString("DefaultLocaleProvider.Variant", displayLocale), //$NON-NLS-1$
            defaultLocale.getVariant(),
            defaultLocale.getDisplayVariant(displayLocale)
        }));

        table.addRow(new Row(new String[] {
            Messages.getString("DefaultLocaleProvider.DisplayName", displayLocale), //$NON-NLS-1$
            defaultLocale.getDisplayName(),
            defaultLocale.getDisplayName(displayLocale)
        }));

        return table;
    }
}
