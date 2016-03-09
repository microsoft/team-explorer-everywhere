// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.rowset;

import java.text.MessageFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.microsoft.tfs.core.clients.workitem.internal.InternalWorkItemConstants;
import com.microsoft.tfs.core.clients.workitem.internal.InternalWorkItemUtils;

/**
 * A table handler that is a base for writing table handlers that need to access
 * specific columns by name and do data type conversion.
 */
public abstract class BaseRowSetHandler implements RowSetParseHandler {
    private final Map<String, Integer> columnNamesToIndexes = new HashMap<String, Integer>();
    private int currentColumnIndex;
    private String[] currentRowValues;
    private String tableName;

    private final SimpleDateFormat dateFormat;

    protected abstract void doHandleRow();

    public BaseRowSetHandler() {
        dateFormat = InternalWorkItemUtils.newMetadataDateFormat();
    }

    @Override
    public void handleBeginParsing() {
        currentColumnIndex = 0;
    }

    @Override
    public void handleTableName(final String tableName) {
        this.tableName = tableName;
    }

    @Override
    public void handleColumn(final String name, final String type) {
        columnNamesToIndexes.put(name, new Integer(currentColumnIndex));
        ++currentColumnIndex;
    }

    @Override
    public void handleFinishedColumns() {
    }

    @Override
    public void handleRow(final String[] rowValues) {
        currentRowValues = rowValues;
        doHandleRow();
    }

    @Override
    public void handleEndParsing() {
    }

    protected Date getDateValue(final String columnName) {
        final String value = getStringValue(columnName);
        if (InternalWorkItemConstants.NULL_DATE_STRING.equals(value)) {
            return null;
        }
        try {
            return dateFormat.parse(value);
        } catch (final ParseException ex) {
            throw new RuntimeException(
                MessageFormat.format(
                    "the value [{0}] from column [{1}] in table [{2}] was not parsable to an date", //$NON-NLS-1$
                    value,
                    columnName,
                    tableName),
                ex);
        }
    }

    protected int getIntValue(final String columnName) {
        final String value = getStringValue(columnName);
        try {
            return Integer.parseInt(value);
        } catch (final NumberFormatException ex) {
            throw new RuntimeException(
                MessageFormat.format(
                    "the value [{0}] from column [{1}] in table [{2}] was not parsable to an int", //$NON-NLS-1$
                    value,
                    columnName,
                    tableName),
                ex);
        }
    }

    protected long getLongValue(final String columnName) {
        final String value = getStringValue(columnName);
        try {
            return Long.parseLong(value);
        } catch (final NumberFormatException ex) {
            throw new RuntimeException(
                MessageFormat.format(
                    "the value [{0}] from column [{1}] in table [{2}] was not parsable to a long", //$NON-NLS-1$
                    value,
                    columnName,
                    tableName),
                ex);
        }
    }

    protected boolean getBooleanValue(final String columnName) {
        final String value = getStringValue(columnName);
        return "true".equalsIgnoreCase(value); //$NON-NLS-1$
    }

    protected String getStringValue(final String columnName) {
        if (!columnNamesToIndexes.containsKey(columnName)) {
            throw new IllegalArgumentException(
                MessageFormat.format(
                    "the column name [{0}] was not encountered in the table [{1}] - columns are: {2}", //$NON-NLS-1$
                    columnName,
                    tableName,
                    columnNamesToIndexes.keySet()));
        }

        final int index = columnNamesToIndexes.get(columnName).intValue();
        return currentRowValues[index];
    }
}
