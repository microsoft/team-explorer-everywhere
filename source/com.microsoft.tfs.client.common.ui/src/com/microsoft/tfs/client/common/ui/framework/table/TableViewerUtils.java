// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.table;

import java.text.MessageFormat;

import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.util.Check;

public class TableViewerUtils {
    /**
     * Creates {@link TableColumn}s for the specified {@link TableViewer} based
     * on the specified {@link TableColumnData} array.
     *
     * @param viewer
     *        the {@link TableViewer} to create columns for (must not be
     *        <code>null</code>)
     * @param headerVisible
     *        <code>true</code> if the {@link Table}'s header should be visible
     * @param linesVisible
     *        <code>true</code> if the {@link Table}'s lines should be visible
     * @param persistenceKey
     *        a persistence key used to save and restore column widths, or
     *        <code>null</code> if no column width persistence should be done
     * @param columnData
     *        the {@link TableColumnData} array that controls the number and
     *        kind of columns that are created (must not be <code>null</code>
     *        and must not contain <code>null</code> elements)
     */
    public static void setupTableViewer(
        final TableViewer viewer,
        final boolean headerVisible,
        final boolean linesVisible,
        final String persistenceKey,
        final TableColumnData[] columnData) {
        Check.notNull(viewer, "viewer"); //$NON-NLS-1$

        TableUtils.setupTable(viewer.getTable(), headerVisible, linesVisible, persistenceKey, columnData);

        final String[] columnProperties = new String[columnData.length];
        boolean setColumnProperties = false;

        for (int i = 0; i < columnData.length; i++) {
            columnProperties[i] = columnData[i].propertyName;
            if (columnProperties[i] != null) {
                setColumnProperties = true;
            }
        }

        if (setColumnProperties) {
            viewer.setColumnProperties(columnProperties);
        }
    }

    /**
     * Converts the specified column index to a column property name. If column
     * property names have not been set on the JFace {@link TableViewer}, or if
     * the specified column index is out of range, <code>null</code> is
     * returned.
     *
     * @param columnIndex
     *        the column index to get the column property name for
     * @return the corresponding column property name, or <code>null</code> if
     *         there is no corresponding column property name
     */
    public static String columnIndexToColumnProperty(final int columnIndex, final TableViewer viewer) {
        final Object[] oColumnProperties = viewer.getColumnProperties();

        if (!(oColumnProperties instanceof String[])) {
            return null;
        }

        final String[] columnProperties = (String[]) oColumnProperties;

        if (columnIndex < 0 || columnIndex >= columnProperties.length) {
            return null;
        }

        return columnProperties[columnIndex];
    }

    /**
     * Converts the specified column property name to a column index. If column
     * property names have not been set on the JFace {@link TableViewer}, or if
     * the specified column property name is not found, <code>-1</code> is
     * returned.
     *
     * @param columnPropertyName
     *        the column property name to get the index for (must not be
     *        <code>null</code>)
     * @return the corresponding column index, or <code>-1</code> if there is no
     *         corresponding column index
     */
    public static final int columnPropertyNameToColumnIndex(final String columnPropertyName, final TableViewer viewer) {
        return columnPropertyNameToColumnIndex(columnPropertyName, false, viewer);
    }

    /**
     * Converts the specified column property name to a column index. If column
     * property names have not been set on the JFace {@link TableViewer}, or if
     * the specified column property name is not found, the
     * <code>mustExist</code> parameter controls the behavior. If
     * <code>mustExist</code> is <code>true</code>, an exception is thrown in
     * these cases.
     *
     * @param columnPropertyName
     *        the column property name to get the index for (must not be
     *        <code>null</code>)
     * @param mustExist
     *        <code>true</code> if an exception should be thrown if the column
     *        property name is not found
     * @return the corresponding column index, or <code>-1</code> if there is no
     *         corresponding column index and <code>mustExist</code> is
     *         <code>false</code>
     */
    public static final int columnPropertyNameToColumnIndex(
        final String columnPropertyName,
        final boolean mustExist,
        final TableViewer viewer) {
        Check.notNull(columnPropertyName, "columnPropertyName"); //$NON-NLS-1$

        final Object[] oColumnProperties = viewer.getColumnProperties();

        if (oColumnProperties instanceof String[]) {
            final String[] columnProperties = (String[]) oColumnProperties;

            for (int i = 0; i < columnProperties.length; i++) {
                if (columnPropertyName.equals(columnProperties[i])) {
                    return i;
                }
            }
        }

        if (mustExist) {
            final String messageFormat = Messages.getString("TableViewerUtils.ColumnPropertyUndefFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, columnPropertyName);
            throw new IllegalArgumentException(message);
        }

        return -1;
    }
}
