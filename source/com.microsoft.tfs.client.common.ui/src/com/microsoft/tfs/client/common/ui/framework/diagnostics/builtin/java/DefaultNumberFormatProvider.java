// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.diagnostics.builtin.java;

import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.util.Locale;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.framework.diagnostics.DiagnosticLocale;
import com.microsoft.tfs.client.common.ui.framework.diagnostics.data.Row;
import com.microsoft.tfs.client.common.ui.framework.diagnostics.data.TabularData;
import com.microsoft.tfs.client.common.ui.framework.diagnostics.extend.DataProvider;

public class DefaultNumberFormatProvider implements DataProvider {
    @Override
    public Object getData() {
        return getData(DiagnosticLocale.USER_LOCALE);
    }

    @Override
    public Object getDataNOLOC() {
        return getData(DiagnosticLocale.SUPPORT_LOCALE);
    }

    private Object getData(final Locale locale) {
        final TabularData table = new TabularData(new String[] {
            Messages.getString("DefaultNumberFormatProvider.ColumnNameFormatType", locale), //$NON-NLS-1$
            Messages.getString("DefaultNumberFormatProvider.ColumnNamePattern", locale), //$NON-NLS-1$
            Messages.getString("DefaultNumberFormatProvider.ColumnNameExample", locale), //$NON-NLS-1$
        });

        addFormat(
            locale,
            NumberFormat.getCurrencyInstance(),
            Messages.getString("DefaultNumberFormatProvider.Currency", locale), //$NON-NLS-1$
            12.34,
            table);

        addFormat(
            locale,
            NumberFormat.getNumberInstance(),
            Messages.getString("DefaultNumberFormatProvider.Number", locale), //$NON-NLS-1$
            12.34,
            table);

        addFormat(
            locale,
            NumberFormat.getIntegerInstance(),
            Messages.getString("DefaultNumberFormatProvider.Integer", locale), //$NON-NLS-1$
            999999999,
            table);

        addFormat(
            locale,
            NumberFormat.getPercentInstance(),
            Messages.getString("DefaultNumberFormatProvider.Percent", locale), //$NON-NLS-1$
            0.12,
            table);

        addFormat(
            locale,
            NumberFormat.getInstance(),
            Messages.getString("DefaultNumberFormatProvider.Default", locale), //$NON-NLS-1$
            12.34,
            table);

        return table;
    }

    private void addFormat(
        final Locale locale,
        final NumberFormat numberFormat,
        final String formatType,
        final double exampleNumber,
        final TabularData table) {
        String pattern;

        if (numberFormat instanceof DecimalFormat) {
            final DecimalFormat decimalFormat = (DecimalFormat) numberFormat;
            pattern = decimalFormat.toPattern();
        } else {
            pattern = MessageFormat.format(
                Messages.getString("DefaultNumberFormatProvider.UnknownClassFormat", locale), //$NON-NLS-1$
                numberFormat.getClass().getName());
        }

        final Row row = new Row(new String[] {
            formatType,
            pattern,
            numberFormat.format(exampleNumber)
        });

        table.addRow(row);
    }
}
