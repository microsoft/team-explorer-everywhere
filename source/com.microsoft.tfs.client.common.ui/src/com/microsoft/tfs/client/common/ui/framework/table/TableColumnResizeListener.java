// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.table;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.widgets.Table;

import com.microsoft.tfs.util.Check;

/**
 * This package listens to {@link Table} paint events, and resizes columns as
 * the table is resized. This is useful to allow a column to shrink or grow to
 * fit the newly alloted table size.
 *
 * This will affect any {@link TableColumnData} with the resizeable flag set.
 *
 * Important note: this will also expand any empty columns (ie, the whitespace
 * at the end.)
 */
public class TableColumnResizeListener implements PaintListener {
    private final TableColumnResizeData[] resizeData;

    /**
     * Width from the last call into our paintControl() method. This allows us
     * to know how the geometry of the table has changed.
     */
    private int lastWidth = -1;

    /**
     * The next column to get any adjustments from remainders.
     */
    private int remainderColumn = 0;

    public TableColumnResizeListener(final TableColumnData[] columnData) {
        Check.notNull(columnData, "columnData"); //$NON-NLS-1$

        /* Construct TableColumnResizeData[] from TableColumnData */
        final List resizeDataList = new ArrayList();
        for (int i = 0; i < columnData.length; i++) {
            if (columnData[i].resizeable) {
                resizeDataList.add(new TableColumnResizeData(i));
            }
        }

        resizeData = (TableColumnResizeData[]) resizeDataList.toArray(new TableColumnResizeData[resizeDataList.size()]);
    }

    @Override
    public void paintControl(final PaintEvent e) {
        final Table table = (Table) e.widget;

        final int currentWidth = table.getClientArea().width;

        /*
         * Our first time getting called is the initial paint, not a resize.
         * Just store the new size for resize calls. (Also ensure that we have
         * resize data to deal with, to avoid div by zero.)
         */
        if (lastWidth < 0 || resizeData.length == 0) {
            getColumnWidths(table);

            lastWidth = currentWidth;
            return;
        }

        final int diff = currentWidth - lastWidth;
        final int diffPerColumn = diff / resizeData.length;
        int diffRemainder = diff % resizeData.length;

        getColumnWidths(table);

        /*
         * Pass one: add diffPerColumn to the width of each resizing column.
         */
        for (int i = 0; i < resizeData.length; i++) {
            adjustColumnWidth(resizeData[i], diffPerColumn);
        }

        /*
         * Pass two: deal with any remaining pixels (that can't be split up
         * equitably amongst resizing columns.)
         */
        while (diffRemainder != 0) {
            final int adjustment = (diffRemainder > 0) ? 1 : -1;

            adjustColumnWidth(resizeData[remainderColumn], adjustment);

            remainderColumn = (remainderColumn + 1) % resizeData.length;
            diffRemainder -= adjustment;
        }

        /* Commit column width changes */
        setColumnWidths(table);

        lastWidth = currentWidth;
    }

    private int getColumnWidths(final Table table) {
        int totalWidth = 0;

        for (int i = 0; i < resizeData.length; i++) {
            resizeData[i].columnWidth = table.getColumn(resizeData[i].columnIndex).getWidth();
            totalWidth += resizeData[i].columnWidth;
        }

        return totalWidth;
    }

    private void adjustColumnWidth(final TableColumnResizeData resizeData, final int diff) {
        resizeData.columnWidth += diff;
    }

    private void setColumnWidths(final Table table) {
        for (int i = 0; i < resizeData.length; i++) {
            table.getColumn(resizeData[i].columnIndex).setWidth(resizeData[i].columnWidth);
        }
    }

    private class TableColumnResizeData {
        public final int columnIndex;

        public int columnWidth = -1;

        public TableColumnResizeData(final int columnIndex) {
            this.columnIndex = columnIndex;
        }
    }
}
