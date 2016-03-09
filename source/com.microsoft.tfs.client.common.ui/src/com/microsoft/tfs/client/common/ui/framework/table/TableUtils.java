// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.table;

import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

import com.microsoft.tfs.util.Check;

/**
 * {@link TableUtils} contains some static utility methods for working with SWT
 * {@link Table}s.
 */
public class TableUtils {
    /**
     * Obtains the highest index of a currently selected item in the specified
     * {@link Table}. If no items are currently selected, returns
     * <code>-1</code>.
     *
     * @param table
     *        the {@link Table} (must not be <code>null</code>)
     * @return the highest index of a selected item, or <code>-1</code> if there
     *         are no selected items
     */
    public static int getMaxSelectionIndex(final Table table) {
        Check.notNull(table, "table"); //$NON-NLS-1$

        final int[] indices = table.getSelectionIndices();

        int max = -1;
        for (int i = 0; i < indices.length; i++) {
            if (indices[i] > max) {
                max = indices[i];
            }
        }

        return max;
    }

    /**
     * Creates {@link TableColumn}s for the specified {@link Table} based on the
     * specified {@link TableColumnData} array.
     *
     * @param table
     *        the {@link Table} to create columns for (must not be
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
    public static void setupTable(
        final Table table,
        final boolean headerVisible,
        final boolean linesVisible,
        final String persistenceKey,
        final TableColumnData[] columnData) {
        Check.notNull(table, "table"); //$NON-NLS-1$
        Check.notNull(columnData, "columnData"); //$NON-NLS-1$

        table.setHeaderVisible(headerVisible);
        table.setLinesVisible(linesVisible);

        TableColumnWidthsPersistence persistenceStore = null;

        /* Clear any existing columns */
        final TableColumn[] existingColumns = table.getColumns();

        for (int i = 0; i < existingColumns.length; i++) {
            existingColumns[i].dispose();
        }

        boolean persist = false;
        for (int i = 0; i < columnData.length; i++) {
            if (columnData[i] == null) {
                throw new IllegalArgumentException("columnData[" + i + "] is null"); //$NON-NLS-1$ //$NON-NLS-2$
            }

            final TableColumn column = new TableColumn(table, columnData[i].style);

            if (columnData[i].text != null) {
                column.setText(columnData[i].text);
            }

            if (columnData[i].image != null) {
                column.setImage(columnData[i].image);
            }

            column.setResizable(columnData[i].resizeable);

            if (columnData[i].persistenceKey != null) {
                persist = true;
            }
        }

        if (persist && persistenceKey != null) {
            persistenceStore = new TableColumnWidthsPersistence(table, persistenceKey);

            for (int i = 0; i < columnData.length; i++) {
                if (columnData[i].persistenceKey != null) {
                    persistenceStore.addMapping(columnData[i].persistenceKey, i);
                }
            }
        }

        table.setLayout(new PersistentTableLayout(columnData, persistenceStore));
        table.layout(true);
    }
}
