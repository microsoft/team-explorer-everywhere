// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.table.tooltip;

import java.lang.reflect.Method;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;

import com.microsoft.tfs.client.common.ui.framework.helper.ControlHelper;
import com.microsoft.tfs.util.Check;

/**
 * Creates a tableitem tooltip manager, which will display tooltips on a per
 * table row or per table cell basis, when a user hovers over them.
 */
public class TableTooltipLabelManager extends TableTooltipManager {
    private final Table table;
    private final TableTooltipLabelProvider labelProvider;

    /* True if tooltips are to be generated per table cell */
    private final boolean supportCellTooltips;

    private boolean tooltipClipped = false;

    /**
     * Creates a table tooltip provider for the given table, using the
     * {@link TableTooltipLabelProvider} to provide tooltip text.
     *
     * You must call {@link TableTooltipLabelManager#addTooltipManager()} to
     * hook up this tooltip manager.
     *
     * @param table
     *        The table to attach the tooltips to
     * @param labelProvider
     *        The label provider to provide tooltip strings
     */
    public TableTooltipLabelManager(
        final Table table,
        final TableTooltipLabelProvider labelProvider,
        final boolean supportCellTooltips) {
        super(table);

        Check.notNull(table, "table"); //$NON-NLS-1$
        Check.notNull(labelProvider, "labelProvider"); //$NON-NLS-1$

        this.table = table;
        this.labelProvider = labelProvider;
        this.supportCellTooltips = supportCellTooltips;
    }

    @Override
    protected boolean shouldReplaceTooltip(
        final TableItem newHoverItem,
        final int newHoverColumnIndex,
        final TableItem oldHoverItem,
        final int oldHoverColumnIndex) {
        /*
         * If we're doing per-row tooltips, then make sure that we're not
         * displaying the helper (clipped) tooltip for this item, because that
         * behaves like per-cell tooltips.
         */
        if (supportCellTooltips || tooltipClipped) {
            return (newHoverItem != oldHoverItem || newHoverColumnIndex != oldHoverColumnIndex);
        }

        return (newHoverItem != oldHoverItem);
    }

    @Override
    protected boolean createTooltip(final Shell shell, final TableItem tableItem, final int columnIndex) {
        tooltipClipped = false;

        if (tableItem == null || tableItem.getData() == null) {
            return false;
        }

        String tooltipText = labelProvider.getTooltipText(tableItem.getData(), columnIndex);

        /*
         * If this table item is being clipped (the width of the row is smaller
         * than the text being displayed) then use the full text as the tooltip.
         */
        if (tooltipText == null && isTableItemClipped(shell, tableItem, columnIndex)) {
            tooltipText = tableItem.getText(columnIndex);
            tooltipClipped = true;
        } else if (tooltipText == null) {
            return false;
        }

        final FillLayout layout = new FillLayout();
        layout.marginWidth = 4;
        layout.marginHeight = 4;
        shell.setLayout(layout);

        final Label tooltipLabel = new Label(shell, SWT.WRAP);
        tooltipLabel.setForeground(shell.getDisplay().getSystemColor(SWT.COLOR_INFO_FOREGROUND));
        tooltipLabel.setBackground(shell.getDisplay().getSystemColor(SWT.COLOR_INFO_BACKGROUND));
        tooltipLabel.setData("_TABLEITEM", tableItem); //$NON-NLS-1$
        tooltipLabel.setText(tooltipText);

        return true;
    }

    private boolean isTableItemClipped(final Shell shell, final TableItem tableItem, final int columnIndex) {
        if (columnIndex == -1) {
            return false;
        }

        /*
         * Measure the text in the table versus the width of the column - if the
         * text is being clipped, we should display that in the tooltip.
         */
        final String columnText = tableItem.getText(columnIndex);

        /*
         * The getTextBounds method is the correct way to measure client width
         * and text bounds for the column, but is only available in 3.3+.
         * Default to the boundaries of the entire column (including image) and
         * column's width (with trimmings) and try to override with reflective
         * invocation of getTextBounds
         */
        Rectangle textBounds = tableItem.getBounds(columnIndex);
        int columnWidth = table.getColumn(columnIndex).getWidth();

        try {
            final Method textBoundsMethod = tableItem.getClass().getMethod("getTextBounds", int.class); //$NON-NLS-1$
            textBounds = (Rectangle) textBoundsMethod.invoke(tableItem, columnIndex);

            columnWidth = textBounds.width;
        } catch (final Throwable t) {
            /* Ignore */
        }

        final GC columnGC = new GC(shell.getDisplay());
        final Point textExtent = columnGC.stringExtent(columnText);

        if (textExtent.x > columnWidth) {
            return true;
        } else {
            /*
             * We can also be clipped when our table column is wide enough to
             * accommodate our text (above), but our table is in a resizable
             * container (like a sash form) that is smaller than our text size.
             * Get the bounds of our table item to get position relative to our
             * receiver, then determine the absolute (display position) of this
             * table item. Get parents absolute display bounds to ensure that
             * this table item's absolute bounds is fully contained inside all
             * of them.
             */
            final Point absoluteStart = table.toDisplay(textBounds.x, textBounds.y);
            final Point absoluteEnd = new Point(absoluteStart.x + textExtent.x, absoluteStart.y + textExtent.y);

            for (Composite parent = table; parent != null && parent.getParent() != null; parent = parent.getParent()) {
                /*
                 * Get the parents bounds in display coordinates. If the parent
                 * has no parent of its own, its bounds are already explicitly
                 * in display coordinates.
                 */
                final Rectangle parentBounds = ControlHelper.getDisplayClientArea(parent);

                if (!parentBounds.contains(absoluteStart) || !parentBounds.contains(absoluteEnd)) {
                    return true;
                }
            }

            return false;
        }
    }
}
