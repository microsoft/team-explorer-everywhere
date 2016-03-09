// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.table;

import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

/**
 * <p>
 * A Layout for use with Tables that divides the available width of a Table
 * equally among all of the columns.
 * </p>
 * <p>
 * Note that this Layout will only set the initial width of the Table columns.
 * After the first time this Layout is called to lay out the Table's columns, it
 * will no longer do anything.
 * </p>
 */
public class EqualSizeTableLayout extends Layout {
    private boolean firstTime = true;

    /**
     * Resets the "first time" state of this EqualSizeTableLayout. If called
     * after this layout has been invoked for the first time to lay out the
     * Table's columns, the next invocation to lay out the columns will perform
     * the same layout as the first time. This method is generally only useful
     * when the first layout occurs at the wrong time - such as under linux/gtk
     * when a table is added to a non-visible tab in a TabFolder.
     */
    public void reset() {
        firstTime = true;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.swt.widgets.Layout#computeSize(org.eclipse.swt.widgets.
     * Composite , int, int, boolean)
     */
    @Override
    protected Point computeSize(final Composite composite, final int wHint, final int hHint, final boolean flushCache) {
        composite.setLayout(null);
        final Point size = composite.computeSize(wHint, hHint, flushCache);
        composite.setLayout(this);
        return size;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.eclipse.swt.widgets.Layout#layout(org.eclipse.swt.widgets.Composite,
     * boolean)
     */
    @Override
    protected void layout(final Composite composite, final boolean flushCache) {
        if (!firstTime) {
            return;
        }

        final int width = composite.getClientArea().width;

        if (width <= 1) {
            return;
        }

        final Table table = (Table) composite;
        final TableColumn[] columns = table.getColumns();

        final int widthPerColumn = width / columns.length;

        table.setLayout(null);

        for (int i = 0; i < columns.length; i++) {
            columns[i].setWidth(widthPerColumn);
        }

        table.setLayout(this);

        firstTime = false;
    }

}
