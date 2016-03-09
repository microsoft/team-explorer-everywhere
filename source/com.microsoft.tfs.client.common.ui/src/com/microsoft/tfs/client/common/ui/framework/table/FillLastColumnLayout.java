// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.table;

import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

public class FillLastColumnLayout extends Layout {
    private Point previousCompositeSize;

    @Override
    protected Point computeSize(final Composite composite, final int wHint, final int hHint, final boolean flushCache) {
        composite.setLayout(null);
        final Point size = composite.computeSize(wHint, hHint, flushCache);
        composite.setLayout(this);
        return size;
    }

    @Override
    protected void layout(final Composite composite, final boolean flushCache) {
        final Rectangle clientArea = composite.getClientArea();

        final Table table = (Table) composite;
        final TableColumn[] columns = table.getColumns();

        final int lastCol = columns.length - 1;
        int widthOfRest = 0;
        for (int i = 0; i < lastCol; i++) {
            widthOfRest += columns[i].getWidth();
        }
        int newWidth = clientArea.width - widthOfRest;
        if (newWidth < 25) {
            newWidth = 25;
        }

        /*
         * To prevent massive stack usage, only set a new width if the
         * composite's total size has changed, not client area. This prevents us
         * grabbing hundreds of (I've seen > 600) stack frames as the scroll
         * bars pop into view when the control is very small, which changes the
         * client area, which forces a resize, which causes us to change the
         * header width, which causes the scroll bars to disappear, which
         * changes the client area, which forces a resize, etc. It eventually
         * settles down, but can blow the stack on 32-bit JVMs.
         */
        if (composite.getSize().equals(previousCompositeSize) == false) {
            previousCompositeSize = composite.getSize();
            columns[lastCol].setWidth(newWidth);
        }
    }
}
