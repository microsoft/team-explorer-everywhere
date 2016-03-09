// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.diagnostics.builtin.java;

import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.framework.diagnostics.data.Row;
import com.microsoft.tfs.client.common.ui.framework.diagnostics.data.TabularData;
import com.microsoft.tfs.client.common.ui.framework.diagnostics.extend.DataProvider;
import com.microsoft.tfs.client.common.ui.framework.diagnostics.extend.LocalizedDataProvider;

public class DefaultDateTimeFormatProvider extends LocalizedDataProvider implements DataProvider {
    @Override
    protected Object getData(final Locale locale) {
        final TabularData table = new TabularData(new String[] {
            Messages.getString("DefaultDateTimeFormatProvider.ColumnNameFormatType", locale), //$NON-NLS-1$
            Messages.getString("DefaultDateTimeFormatProvider.ColumnNameDateStyle", locale), //$NON-NLS-1$
            Messages.getString("DefaultDateTimeFormatProvider.ColumnNameTimeStyle", locale), //$NON-NLS-1$
            Messages.getString("DefaultDateTimeFormatProvider.ColumnNamePattern", locale), //$NON-NLS-1$
            Messages.getString("DefaultDateTimeFormatProvider.ColumnNameExample", locale) //$NON-NLS-1$
        });

        final Date exampleDate = new Date();

        for (int style = DateFormat.FULL; style <= DateFormat.SHORT; style++) {
            final DateFormat dateFormat = DateFormat.getDateInstance(style);
            addFormat(
                locale,
                dateFormat,
                Messages.getString("DefaultDateTimeFormatProvider.Date", locale), //$NON-NLS-1$
                style,
                -1,
                exampleDate,
                table);
        }

        for (int style = DateFormat.FULL; style <= DateFormat.SHORT; style++) {
            final DateFormat dateFormat = DateFormat.getTimeInstance(style);
            addFormat(
                locale,
                dateFormat,
                Messages.getString("DefaultDateTimeFormatProvider.Time", locale), //$NON-NLS-1$
                -1,
                style,
                exampleDate,
                table);
        }

        for (int dateStyle = DateFormat.FULL; dateStyle <= DateFormat.SHORT; dateStyle++) {
            for (int timeStyle = DateFormat.FULL; timeStyle <= DateFormat.SHORT; timeStyle++) {
                final DateFormat dateFormat = DateFormat.getDateTimeInstance(dateStyle, timeStyle);
                addFormat(
                    locale,
                    dateFormat,
                    Messages.getString("DefaultDateTimeFormatProvider.DateTime", locale), //$NON-NLS-1$
                    dateStyle,
                    timeStyle,
                    exampleDate,
                    table);
            }
        }

        return table;
    }

    private void addFormat(
        final Locale locale,
        final DateFormat dateFormat,
        final String formatType,
        final int dateStyle,
        final int timeStyle,
        final Date exampleDate,
        final TabularData table) {
        String pattern;

        if (dateFormat instanceof SimpleDateFormat) {
            final SimpleDateFormat sdf = (SimpleDateFormat) dateFormat;
            pattern = sdf.toPattern();
        } else {
            pattern =
                MessageFormat.format(
                    Messages.getString("DefaultDateTimeFormatProvider.UnknownClassFormat", locale), //$NON-NLS-1$
                    dateFormat.getClass().getName());
        }

        final Row row = new Row(new String[] {
            formatType,
            styleConstantToString(locale, dateStyle),
            styleConstantToString(locale, timeStyle),
            pattern,
            dateFormat.format(exampleDate)
        });

        table.addRow(row);
    }

    private String styleConstantToString(final Locale locale, final int style) {
        switch (style) {
            case -1:
                return ""; //$NON-NLS-1$
            case 0:
                return Messages.getString("DefaultDateTimeFormatProvider.StyleFull", locale); //$NON-NLS-1$
            case 1:
                return Messages.getString("DefaultDateTimeFormatProvider.StyleLong", locale); //$NON-NLS-1$
            case 2:
                return Messages.getString("DefaultDateTimeFormatProvider.StyleMedium", locale); //$NON-NLS-1$
            case 3:
                return Messages.getString("DefaultDateTimeFormatProvider.StyleShort", locale); //$NON-NLS-1$
            default:
                return Messages.getString("DefaultDateTimeFormatProvider.StyleUnknown", locale); //$NON-NLS-1$
        }
    }
}
