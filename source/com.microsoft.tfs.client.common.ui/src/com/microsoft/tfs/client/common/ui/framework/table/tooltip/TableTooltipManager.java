// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.table.tooltip;

import java.lang.reflect.Field;
import java.util.Timer;
import java.util.TimerTask;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;

import com.microsoft.tfs.client.common.ui.framework.WindowSystem;
import com.microsoft.tfs.util.Check;

/**
 * This provides a generic implementation of per-cell tooltips for tables. You
 * are expected to subclass this and fill a tooltip shell. This allows for
 * generic, complex tooltip support. For a concrete implementation that simply
 * provides textual tooltips, see {@link TableTooltipLabelManager}.
 *
 * Note: you may think this looks unnecessarily complex and have a temptation to
 * use Table.setTooltipText() in a mouse hover listener. Please don't. The
 * problem is that mouse hover events are fired independently of tooltip raises.
 * Further, this is wholly platform dependent. On Mac OS, you'll be setting
 * tooltip text after it would have been raised. On Windows, you may get
 * reasonable results, but intermittent because you're resetting the text at
 * inappropriate times.
 */
public abstract class TableTooltipManager {
    private final Table table;

    private boolean optionMoveWithMouse = true;

    /* The shell containing the tooltip */
    private Shell tooltipShell = null;

    /* The item that is (or was) hovered over. */
    private TableItem hoverItem = null;

    /* The index of the column that is (or was) hovered over. */
    private int hoverColumnIndex = -1;

    /* A timer to close the tooltip after a few seconds */
    private Timer hoverClearTimer = null;

    /* Max size of a tooltip */
    private Point maxTooltipSize = null;

    /*
     * SWT MouseWheel event: must get this reflectively since SWT 3.0 lacks it
     * :(
     */
    private int SWT_MouseWheel = -1;

    /**
     * @return the default tooltip timeout in milliseconds for the current
     *         platform
     */
    public static int getPlatformDefaultTooltipTimeout() {
        if (WindowSystem.isCurrentWindowSystem(WindowSystem.AQUA)) {
            return 10000;
        } else if (WindowSystem.isCurrentWindowSystem(WindowSystem.WINDOWS)) {
            return 5000;
        } else {
            return 7500;
        }
    }

    public TableTooltipManager(final Table table) {
        Check.notNull(table, "table"); //$NON-NLS-1$

        this.table = table;

        /* Get the SWT.MouseWheel field reflectively for RAD 6 compat */
        try {
            final Field mouseWheelField = SWT.class.getField("MouseWheel"); //$NON-NLS-1$
            final Integer mouseWheelValue = (Integer) mouseWheelField.get(null);
            SWT_MouseWheel = mouseWheelValue.intValue();
        } catch (final Exception e) {
        }
    }

    public void addTooltipManager() {
        table.setToolTipText(""); //$NON-NLS-1$

        table.addListener(SWT.MouseHover, tooltipEventListener);
        table.addListener(SWT.MouseMove, tooltipEventListener);

        table.addListener(SWT.Dispose, tooltipEventListener);
        table.addListener(SWT.MouseDown, tooltipEventListener);
        table.addListener(SWT.MouseUp, tooltipEventListener);
        table.addListener(SWT.MouseDoubleClick, tooltipEventListener);
        table.addListener(SWT.MouseEnter, tooltipEventListener);
        table.addListener(SWT.MouseExit, tooltipEventListener);
        table.addListener(SWT.FocusOut, tooltipEventListener);
        table.addListener(SWT.FocusIn, tooltipEventListener);
        table.addListener(SWT.Selection, tooltipEventListener);

        if (SWT_MouseWheel >= 0) {
            table.addListener(SWT_MouseWheel, tooltipEventListener);
        }
    }

    public void removeTooltipManager() {
        table.removeListener(SWT.MouseHover, tooltipEventListener);
        table.removeListener(SWT.MouseMove, tooltipEventListener);

        table.removeListener(SWT.Dispose, tooltipEventListener);
        table.removeListener(SWT.MouseDown, tooltipEventListener);
        table.removeListener(SWT.MouseUp, tooltipEventListener);
        table.removeListener(SWT.MouseDoubleClick, tooltipEventListener);
        table.removeListener(SWT.MouseEnter, tooltipEventListener);
        table.removeListener(SWT.MouseExit, tooltipEventListener);
        table.removeListener(SWT.FocusOut, tooltipEventListener);
        table.removeListener(SWT.FocusIn, tooltipEventListener);
        table.removeListener(SWT.Selection, tooltipEventListener);

        if (SWT_MouseWheel >= 0) {
            table.addListener(SWT_MouseWheel, tooltipEventListener);
        }

        table.setToolTipText(null);
    }

    /**
     * If this option is true, the tooltip will follow mouse movement (provided
     * the mouse stays over the same table item they hovered on. If this is
     * false the tooltip will close on a mouse move.
     *
     * @param optionMoveWithMouse
     *        true to follow the mouse, false to dispose tooltip
     */
    protected void setOptionMoveWithMouse(final boolean optionMoveWithMouse) {
        this.optionMoveWithMouse = optionMoveWithMouse;
    }

    protected boolean getOptionMoveWithMouse() {
        return optionMoveWithMouse;
    }

    private final Listener tooltipEventListener = new Listener() {
        @Override
        public void handleEvent(final Event event) {
            if (table.isDisposed()) {
                return;
            }

            switch (event.type) {
                case SWT.MouseHover:
                    final TableItem newHoverItem = table.getItem(new Point(event.x, event.y));

                    if (tooltipShell != null && !tooltipShell.isDisposed()) {
                        return;
                    }

                    if (newHoverItem == null) {
                        return;
                    }

                    /*
                     * We still need to calculate the hover column index, even
                     * when we're not in cell-based tooltip mode. This will
                     * allow us to paint clipped text for a column even for
                     * row-based tooltip contributors.
                     */
                    final int newHoverColumnIndex = getColumnIndex(newHoverItem, event.x, event.y);

                    /*
                     * Allow subclasses to veto the tooltip change. (They may
                     * provide per-row tooltips instead of per-column.)
                     */
                    if (!shouldReplaceTooltip(newHoverItem, newHoverColumnIndex, hoverItem, hoverColumnIndex)) {
                        return;
                    }

                    /* Update the hover fields */
                    hoverItem = newHoverItem;
                    hoverColumnIndex = newHoverColumnIndex;

                    final Display display = table.getDisplay();

                    tooltipShell = new Shell(table.getShell(), SWT.ON_TOP | SWT.NO_FOCUS | SWT.TOOL);
                    tooltipShell.setBackground(display.getSystemColor(SWT.COLOR_INFO_BACKGROUND));

                    if (!createTooltip(tooltipShell, hoverItem, hoverColumnIndex)) {
                        tooltipShell = null;
                        return;
                    }

                    final Point tooltipSize = getTooltipSize(tooltipShell);
                    final Point tooltipLocation = getTooltipLocation(event.x, event.y);

                    tooltipShell.setBounds(tooltipLocation.x, tooltipLocation.y, tooltipSize.x, tooltipSize.y);
                    tooltipShell.setVisible(true);

                    /* Set up a Timer to clear this tooltip */
                    hoverClearTimer = new Timer();
                    hoverClearTimer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            display.asyncExec(new Runnable() {
                                @Override
                                public void run() {
                                    if (table != null && !table.isDisposed()) {
                                        closeTooltip();
                                    }
                                }
                            });
                        }
                    }, getTooltipTimeout());

                    break;

                case SWT.MouseMove:
                    final TableItem moveItem = table.getItem(new Point(event.x, event.y));
                    final int moveColumnIndex = getColumnIndex(moveItem, event.x, event.y);

                    /* No tooltip open */
                    if (hoverItem == null) {
                        return;
                    }

                    /* Moved off the table item - clear tool tip */
                    if (shouldReplaceTooltip(moveItem, moveColumnIndex, hoverItem, hoverColumnIndex)) {
                        closeTooltip();
                        hoverItem = null;
                        return;
                    }

                    /* Moved the mouse, move the tooltip */
                    else if (optionMoveWithMouse && tooltipShell != null && !tooltipShell.isDisposed()) {
                        tooltipShell.setLocation(getTooltipLocation(event.x, event.y));
                    }

                    break;

                case SWT.MouseDown:
                case SWT.MouseUp:
                    closeTooltip();
                    break;

                case SWT.Selection:
                    final TableItem[] selection = table.getSelection();

                    if (selection.length != 1) {
                        closeTooltip();
                    } else if (selection[0] != hoverItem) {
                        closeTooltip();
                    }

                    break;

                case SWT.Dispose:
                    removeTooltipManager();
                    /* Fall-through to close tooltip */

                default:
                    closeTooltip();
            }
        }
    };

    protected Point getTooltipSize(final Shell tooltipShell) {
        if (maxTooltipSize == null) {
            final GC gc = new GC(tooltipShell);

            final int max =
                Dialog.convertVerticalDLUsToPixels(gc.getFontMetrics(), IDialogConstants.MINIMUM_MESSAGE_AREA_WIDTH);
            maxTooltipSize = new Point(max, max);

            gc.dispose();
        }

        Point tooltipSize = tooltipShell.computeSize(SWT.DEFAULT, SWT.DEFAULT);

        if (tooltipSize.x > maxTooltipSize.x) {
            tooltipSize = tooltipShell.computeSize(maxTooltipSize.x, SWT.DEFAULT);
        }
        if (tooltipSize.y > maxTooltipSize.y) {
            tooltipSize = tooltipShell.computeSize(maxTooltipSize.x, maxTooltipSize.y);
        }

        return tooltipSize;
    }

    /**
     * Determines the location of the tooltip, given the current mouse
     * coordinates. Subclasses may override.
     *
     * @param cursorX
     *        X position of the mouse cursor
     * @param cursorY
     *        Y position of the mouse cursor
     * @return A Point to draw the tooltip at
     */
    protected Point getTooltipLocation(final int cursorX, final int cursorY) {
        final Point tooltipLocation = table.toDisplay(cursorX, cursorY);

        final Point[] cursorSizes = table.getDisplay().getCursorSizes();
        if (cursorSizes != null && cursorSizes.length > 0 && WindowSystem.isCurrentWindowSystem(WindowSystem.AQUA)) {
            tooltipLocation.x += cursorSizes[0].x;
            tooltipLocation.y += cursorSizes[0].y;
        } else if (cursorSizes != null && cursorSizes.length > 0) {
            tooltipLocation.x += (cursorSizes[0].x / 2);
            tooltipLocation.y += (cursorSizes[0].y / 2);
        } else {
            tooltipLocation.x += 7;
            tooltipLocation.y += 15;
        }

        return tooltipLocation;
    }

    /**
     * This is the time when tooltips will clear themselves (in ms.) After a
     * tooltip is raised, it will close itself after several seconds, depending
     * on the platform. Subclasses may override.
     *
     * @return ms to clear the timeout in
     */
    protected int getTooltipTimeout() {
        return getPlatformDefaultTooltipTimeout();
    }

    /**
     * Subclasses may override to veto tooltip replacement on a per-item or
     * per-cell basis. The default implementation replaces tooltips on a
     * per-cell basis. This allows subclasses to avoid flicker when moving
     * between cells when the tooltip should stay up.
     *
     * @param newHoverItem
     *        The item currently being hovered over
     * @param newHoverColumnIndex
     *        The column index currently being hovered over
     * @param oldHoverItem
     *        The item that was previously being hovered over (may be equal to
     *        newHoverItem)
     * @param oldHoverColumnIndex
     *        The column index currently being hovered over (may be equal to
     *        newHoverColumnIndex)
     * @return true to replace the tooltip, false if it would be identical
     */
    protected boolean shouldReplaceTooltip(
        final TableItem newHoverItem,
        final int newHoverColumnIndex,
        final TableItem oldHoverItem,
        final int oldHoverColumnIndex) {
        /* Replace the tooltip if the item or column index is different. */
        return (newHoverItem != hoverItem || newHoverColumnIndex != hoverColumnIndex);
    }

    /**
     * Subclasses must subclass this to populate the tooltip. If the subclass
     * returns false, no tooltip will be displayed for this table item.
     *
     * @param shell
     *        The shell to populate with tooltip data
     * @param tableItem
     *        The TableItem that this tooltip is for
     * @param columnIndex
     *        The column index of the cell or -1 if cell tips are not supported
     * @return true if the shell was populated and should be raised as a
     *         tooltip, false to suppress tooltip
     */
    protected abstract boolean createTooltip(Shell shell, TableItem tableItem, int columnIndex);

    public void closeTooltip() {
        if (tooltipShell == null) {
            return;
        }

        if (!tooltipShell.isDisposed()) {
            tooltipShell.close();
            tooltipShell.dispose();
        }

        tooltipShell = null;

        if (hoverClearTimer != null) {
            hoverClearTimer.cancel();
            hoverClearTimer = null;
        }
    }

    /**
     * Find the column index which contains the specified point in the specified
     * table item.
     *
     * @return The index of the column containing the specified point or -1 if
     *         the point is not in any column
     */
    private int getColumnIndex(final TableItem item, final int x, final int y) {
        if (item != null) {
            for (int i = 0; i < table.getColumnCount(); i++) {
                final Rectangle rect = item.getBounds(i);

                if (rect.contains(x, y)) {
                    return i;
                }
            }
        }
        return -1;
    }
}
