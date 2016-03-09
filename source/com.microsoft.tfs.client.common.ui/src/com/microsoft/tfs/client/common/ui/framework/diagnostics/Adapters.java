// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.diagnostics;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Properties;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.framework.diagnostics.data.PropertyValueTable;
import com.microsoft.tfs.client.common.ui.framework.diagnostics.data.Row;
import com.microsoft.tfs.client.common.ui.framework.diagnostics.data.TabularData;

public class Adapters {
    public static Object get(final Object data, final Class desired) {
        return get(data, desired, true);
    }

    public static Object get(final Object data, final Class desired, final boolean localeSpecific) {
        if (data == null) {
            return null;
        }

        if (desired.isAssignableFrom(data.getClass())) {
            return data;
        }

        if (String.class.equals(desired)) {
            final String s = attemptBuiltInStringConversion(data, localeSpecific);
            if (s != null) {
                return s;
            }
        } else if (TabularData.class.equals(desired)) {
            final TabularData table = attemptBuiltInTabularDataConversion(data, localeSpecific);
            if (table != null) {
                return table;
            }
        }

        return null;
    }

    private static TabularData attemptBuiltInTabularDataConversion(final Object data, final boolean localeSpecific) {
        if (data instanceof Properties) {
            final Locale locale = localeSpecific ? DiagnosticLocale.USER_LOCALE : DiagnosticLocale.SUPPORT_LOCALE;

            final Properties properties = (Properties) data;
            final PropertyValueTable table = new PropertyValueTable(locale);
            table.addAll(properties);
            return table;
        }
        if (data instanceof File && ((File) data).isFile()) {
            return convertFileToTable((File) data, localeSpecific);
        }
        return null;
    }

    private static TabularData convertFileToTable(final File file, final boolean localeSpecific) {
        final Locale locale = localeSpecific ? DiagnosticLocale.USER_LOCALE : DiagnosticLocale.SUPPORT_LOCALE;

        final TabularData table = new TabularData(new String[] {
            Messages.getString("Adapters.ColumnNameName", locale), //$NON-NLS-1$
            Messages.getString("Adapters.ColumnNameSize", locale), //$NON-NLS-1$
            Messages.getString("Adapters.ColumnNameLastModified", locale) //$NON-NLS-1$
        });

        table.addRow(new Row(new Object[] {
            file.getName(),
            String.valueOf(file.length()),
            new Date(file.lastModified())
        }, file));

        return table;
    }

    private static String attemptBuiltInStringConversion(final Object data, final boolean localeSpecific) {
        if (isPrimitiveWrapper(data.getClass())) {
            return String.valueOf(data);
        }
        if (data instanceof Date) {
            final Date date = (Date) data;
            DateFormat dateFormat;
            if (localeSpecific) {
                dateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.LONG);
            } else {
                dateFormat = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss z"); //$NON-NLS-1$
            }
            return dateFormat.format(date);
        }
        if (data instanceof File) {
            final File file = (File) data;
            return file.getAbsolutePath();
        }
        if (data instanceof TabularData) {
            return tabularDataToString((TabularData) data, localeSpecific);
        }
        if (data instanceof Properties) {
            final TabularData tabularData = attemptBuiltInTabularDataConversion(data, localeSpecific);
            return tabularDataToString(tabularData, localeSpecific);
        }
        return null;
    }

    private static boolean isPrimitiveWrapper(final Class c) {
        return Boolean.class.equals(c)
            || Byte.class.equals(c)
            || Character.class.equals(c)
            || Short.class.equals(c)
            || Integer.class.equals(c)
            || Long.class.equals(c)
            || Float.class.equals(c)
            || Double.class.equals(c);
    }

    private static String tabularDataToString(final TabularData table, final boolean localeSpecific) {
        if (!table.isSorted()) {
            table.sortByFirstColumn();
        }

        final StringBuffer sb = new StringBuffer();
        final String newline = System.getProperty("line.separator"); //$NON-NLS-1$

        final String[] columns = table.getColumns();
        final String[][] data = makeStringData(table.getRows(), columns.length, localeSpecific);
        final int[] columnWidths = computeColumnWidths(data, columns);

        for (int colIndex = 0; colIndex < columns.length; colIndex++) {
            sb.append(pad(columns[colIndex], columnWidths[colIndex], ' '));
            sb.append("    "); //$NON-NLS-1$
        }
        sb.append(newline);

        for (int colIndex = 0; colIndex < columns.length; colIndex++) {
            sb.append(pad("", columnWidths[colIndex], '-')); //$NON-NLS-1$
            sb.append("    "); //$NON-NLS-1$
        }
        sb.append(newline);

        for (int rowIndex = 0; rowIndex < data.length; rowIndex++) {
            for (int colIndex = 0; colIndex < columns.length; colIndex++) {
                sb.append(pad(data[rowIndex][colIndex], columnWidths[colIndex], ' '));
                sb.append("    "); //$NON-NLS-1$
            }
            sb.append(newline);
        }

        return sb.toString();
    }

    private static String pad(final String input, final int padLength, final char padChar) {
        if (input.length() > padLength) {
            return input;
        }

        final StringBuffer sb = new StringBuffer(input);

        final int numToPad = padLength - input.length();
        for (int i = 0; i < numToPad; i++) {
            sb.append(padChar);
        }

        return sb.toString();
    }

    private static int[] computeColumnWidths(final String[][] data, final String[] columns) {
        final int[] columnWidths = new int[columns.length];

        for (int colIndex = 0; colIndex < columns.length; colIndex++) {
            int length = 0;
            length = Math.max(length, columns[colIndex].length());
            for (int rowIndex = 0; rowIndex < data.length; rowIndex++) {
                length = Math.max(length, data[rowIndex][colIndex].length());
            }
            columnWidths[colIndex] = Math.min(length, 80);
        }

        return columnWidths;
    }

    private static String[][] makeStringData(final Row[] rows, final int numColumns, final boolean localeSpecific) {
        final String[][] data = new String[rows.length][numColumns];

        for (int rowIndex = 0; rowIndex < rows.length; rowIndex++) {
            final Object[] valuesForRow = rows[rowIndex].getValues();
            for (int colIndex = 0; colIndex < numColumns; colIndex++) {
                String text = (String) Adapters.get(valuesForRow[colIndex], String.class, localeSpecific);
                if (text == null) {
                    text = ""; //$NON-NLS-1$
                }
                data[rowIndex][colIndex] = text;
            }
        }

        return data;
    }
}
