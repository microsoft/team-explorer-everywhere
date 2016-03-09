// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.table;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

import com.microsoft.tfs.client.common.ui.framework.sizing.ControlSize;
import com.microsoft.tfs.util.Check;

/**
 * PersistentTableLayout allows you to configure table column widths based on
 * pixel width or percentage of the available table width, and allows for
 * persisting widths between table invocations. This is liberally borrowed from
 * SWT TableLayout, with some persistence code.
 */
public class PersistentTableLayout extends Layout {
    /**
     * The column data which contains width instructions.
     */
    private final TableColumnData[] columnData;

    /**
     * Optional persistence store for the column widths
     */
    private final TableColumnWidthsPersistence persistenceStore;

    /**
     * Indicates whether <code>layout</code> has yet to be called.
     */
    private boolean firstTime = true;

    public PersistentTableLayout(final TableColumnData[] columnData) {
        this(columnData, null);
    }

    public PersistentTableLayout(
        final TableColumnData[] columnData,
        final TableColumnWidthsPersistence persistenceStore) {
        Check.notNull(columnData, "columnData"); //$NON-NLS-1$

        this.columnData = columnData;
        this.persistenceStore = persistenceStore;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.swt.widgets.Layout#computeSize(org.eclipse.swt.widgets.
     * Composite , int, int, boolean)
     */
    @Override
    public Point computeSize(final Composite composite, final int wHint, final int hHint, final boolean flush) {
        if (wHint != SWT.DEFAULT && hHint != SWT.DEFAULT) {
            return new Point(wHint, hHint);
        }

        final Table table = (Table) composite;

        /* Remove ourselves to avoid recursions */
        table.setLayout(null);
        final Point tableSize = table.computeSize(wHint, hHint, flush);
        table.setLayout(this);

        /* Get the font metrics for the table to handle charWidth */
        final FontMetrics fontMetrics = ControlSize.getFontMetrics(composite);

        int minWidth = 0;

        for (int i = 0; i < columnData.length; i++) {
            int persistWidth = -1;

            if (persistenceStore != null && columnData[i].persistenceKey != null) {
                persistWidth = persistenceStore.getColumnWidth(columnData[i].persistenceKey);
            }

            if (persistWidth >= 0) {
                minWidth += persistWidth;
            } else if (columnData[i].width >= 0) {
                minWidth += columnData[i].width;
            } else if (columnData[i].charWidth >= 0) {
                minWidth += columnData[i].charWidth * fontMetrics.getAverageCharWidth();
            }
        }

        if (minWidth > tableSize.x) {
            tableSize.x = minWidth;
        }

        return tableSize;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.eclipse.swt.widgets.Layout#layout(org.eclipse.swt.widgets.Composite,
     * boolean)
     */
    @Override
    public void layout(final Composite composite, final boolean flush) {
        /* We only want to layout the first time. */
        if (!firstTime) {
            return;
        }

        /* Get the width we're able to fill */
        final int tableWidth = composite.getClientArea().width;

        /* Get the font metrics for the table to handle charWidth */
        final FontMetrics fontMetrics = ControlSize.getFontMetrics(composite);

        /*
         * Note: TableLayout() suggests that layout() is called inappropriately
         * early on Linux. The comments suggest that this will allow it to be
         * called at an appropriate time.
         */
        if (tableWidth <= 1) {
            return;
        }

        /* Get the columns */
        final Item[] columns = getColumns(composite);

        final int numColumns = Math.min(columnData.length, columns.length);

        /* Width data to be set after total computation */
        final int width[] = new int[numColumns];
        int columnWidths = 0;

        /* Do we have columns with a percentWidth field? */
        boolean percentColumns = false;

        /*
         * First pass: restore any persisted settings, and set any fixed-width
         * columns.
         */
        for (int i = 0; i < numColumns; i++) {
            int persistWidth = -1;

            if (persistenceStore != null && columnData[i].persistenceKey != null) {
                persistWidth = persistenceStore.getColumnWidth(columnData[i].persistenceKey);
            }

            /*
             * Bug 1827: TableColumnWidthsPersistence sets width to zero when
             * the table isn't drawn properly. Work around that here.
             */
            if (persistWidth > 0) {
                width[i] = persistWidth;
            } else if (columnData[i].width > 0) {
                width[i] = columnData[i].width;
            } else if (columnData[i].charWidth > 0) {
                width[i] = columnData[i].charWidth * fontMetrics.getAverageCharWidth();
            } else {
                width[i] = (tableWidth / numColumns);
            }

            if (columnData[i].percentWidth >= 0) {
                percentColumns = true;
            }

            columnWidths += width[i];
        }

        /* Second pass: add any weights based on the widthPercent field */
        if (tableWidth > columnWidths && percentColumns) {
            final int remainder = tableWidth - columnWidths;

            for (int i = 0; i < numColumns; i++) {
                if (columnData[i].percentWidth >= 0) {
                    width[i] += (int) (remainder * columnData[i].percentWidth);
                }
            }
        }

        /* Last pass: assign the widths to the columns */
        for (int i = 0; i < numColumns; i++) {
            setWidth(columns[i], width[i]);
        }

        firstTime = false;
    }

    /**
     * Set the width of the item.
     *
     * @param item
     * @param width
     */
    private void setWidth(final Item item, final int width) {
        if (item instanceof TableColumn) {
            ((TableColumn) item).setWidth(width);
        }
    }

    /**
     * Return the columns for the receiver.
     *
     * @param composite
     * @return Item[]
     */
    private Item[] getColumns(final Composite composite) {
        if (composite instanceof Table) {
            return ((Table) composite).getColumns();
        }

        return new Item[0];
    }
}
