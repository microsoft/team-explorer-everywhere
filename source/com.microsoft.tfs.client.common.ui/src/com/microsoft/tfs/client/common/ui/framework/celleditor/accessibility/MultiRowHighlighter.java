// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.celleditor.accessibility;

import java.lang.reflect.Method;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.FocusCellHighlighter;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerRow;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Widget;

/**
 * A cell highlighter capable of handling multi-select tables properly.
 *
 * @threadsafety unknown
 */
public class MultiRowHighlighter extends FocusCellHighlighter {
    private static final Log log = LogFactory.getLog(MultiRowHighlighter.class);

    public MultiRowHighlighter(final ColumnViewer viewer) {
        super(viewer);

        viewer.getControl().addListener(SWT.EraseItem, new MultiRowEraseListener(viewer, this));
    }

    @Override
    protected void focusCellChanged(final ViewerCell newCell, final ViewerCell oldCell) {
        super.focusCellChanged(newCell, oldCell);

        /*
         * When the focus is changed, we merely tell the table to redraw the
         * affected row(s).
         */
        if (newCell != null) {
            newCell.getViewerRow().getControl().redraw();
        }

        if (oldCell != null) {
            oldCell.getViewerRow().getControl().redraw();
        }
    }

    private static class MultiRowEraseListener implements Listener {
        private final ColumnViewer viewer;
        private final MultiRowHighlighter cellHighlighter;

        public MultiRowEraseListener(final ColumnViewer viewer, final MultiRowHighlighter cellHighlighter) {
            this.viewer = viewer;
            this.cellHighlighter = cellHighlighter;
        }

        @Override
        public void handleEvent(final Event event) {
            if ((event.detail & SWT.SELECTED) == 0) {
                return;
            }

            /*
             * Try to use reflection to call ColumnViewer#getRowForItem, a
             * package-protected method.
             */

            ViewerRow selectedRow;

            try {
                final Method method = viewer.getClass().getDeclaredMethod("getViewerRowFromItem", new Class[] //$NON-NLS-1$
                {
                    Widget.class
                });
                method.setAccessible(true);

                selectedRow = (ViewerRow) method.invoke(viewer, new Object[] {
                    event.item
                });
            } catch (final Exception e) {
                log.warn("Could not get row from selected item", e); //$NON-NLS-1$
                return;
            }

            final ViewerCell paintingCell = selectedRow.getCell(event.index);
            final ViewerCell focusedCell = cellHighlighter.getFocusCell();

            /*
             * See if this particular cell is focused
             */
            boolean selected = (paintingCell.equals(focusedCell));

            /*
             * If this cell is not focused, see if it's a cell in a row that is
             * one of many selected rows.
             */
            if (!selected
                && viewer.getSelection() != null
                && viewer.getSelection() instanceof IStructuredSelection
                && paintingCell.getViewerRow().getElement() != null) {
                final IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();

                if (selection.size() > 1) {
                    for (final Iterator i = selection.iterator(); i.hasNext();) {
                        if (paintingCell.getViewerRow().getElement().equals(i.next())) {
                            selected = true;
                            break;
                        }
                    }
                }
            }

            /*
             * Paint selection if the current cell is focused *or* multiple rows
             * are selected. (Our focus cell changed listener, above, limits us
             * to being called for only the selected row(s).) If there's a
             * single row selected and we're not being called to paint the focus
             * cell, then paint those with normal color.
             */
            final int foregroundId = selected ? SWT.COLOR_LIST_SELECTION_TEXT : SWT.COLOR_LIST_FOREGROUND;
            final int backgroundId = selected ? SWT.COLOR_LIST_SELECTION : SWT.COLOR_LIST_BACKGROUND;

            event.gc.setForeground(event.gc.getDevice().getSystemColor(foregroundId));
            event.gc.setBackground(event.gc.getDevice().getSystemColor(backgroundId));

            final int clientWidth = ((Table) viewer.getControl()).getClientArea().width;
            event.gc.fillRectangle(0, event.y, clientWidth, event.height);

            /* Veto future events (ie the focus cell highlighter) */
            event.detail &= ~SWT.SELECTED;
        }
    }
}
