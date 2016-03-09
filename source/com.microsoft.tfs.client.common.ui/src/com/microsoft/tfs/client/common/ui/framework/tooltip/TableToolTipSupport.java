// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.tooltip;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;

/**
 * This class provides tool tip support for items in a table.
 * <p>
 * Currently this support is used only for the links and file attachments table
 * in the WIT GUI, so this class has been customized for that situation.
 * <p>
 * This class was derived by starting with SWT snippet 125 and then customizing
 * to get a closer look and feel to Visual Studio:
 * <ul>
 * <li>the tooltip appears only when hovering in the first column of the table
 * </li>
 * <li>the tooltip has a 5 px margin</li>
 * <li>the tooltip appears as an offset from the mouse position instead of as an
 * offset from the table column location (as in snippet 125)</li>
 * </ul>
 * <p>
 * If a more general-purpose tool tip helper for tables is needed, this class
 * could be a starting point for a more generalized helper class.
 */
public class TableToolTipSupport {
    public static void addToolTipSupport(final Table table, final Shell shell, final IToolTipProvider toolTipProvider) {
        // Disable native tooltip
        table.setToolTipText(""); //$NON-NLS-1$

        // Implement a "fake" tooltip
        final Listener labelListener = new Listener() {
            @Override
            public void handleEvent(final Event event) {
                final Label label = (Label) event.widget;
                final Shell shell = label.getShell();
                switch (event.type) {
                    case SWT.MouseDown:
                        final Event e = new Event();
                        e.item = (TableItem) label.getData("_TABLEITEM"); //$NON-NLS-1$
                        // Assuming table is single select, set the selection as
                        // if
                        // the mouse down event went through to the table
                        table.setSelection(new TableItem[] {
                            (TableItem) e.item
                        });
                        table.notifyListeners(SWT.Selection, e);
                        // fall through
                    case SWT.MouseExit:
                        shell.dispose();
                        break;
                }
            }
        };

        final Listener tableListener = new Listener() {
            Shell tip = null;
            Label label = null;

            @Override
            public void handleEvent(final Event event) {
                switch (event.type) {
                    case SWT.Dispose:
                    case SWT.KeyDown:
                    case SWT.MouseMove: {
                        if (tip == null) {
                            break;
                        }
                        tip.dispose();
                        tip = null;
                        label = null;
                        break;
                    }
                    case SWT.MouseHover: {
                        final Point eventPoint = new Point(event.x, event.y);
                        final TableItem item = table.getItem(eventPoint);
                        if (item != null) {
                            /*
                             * if the mouse hover didn't happen inside the first
                             * column, bail out
                             */
                            final Rectangle rect = item.getBounds(0);
                            if (!rect.contains(eventPoint)) {
                                return;
                            }

                            final Object element = item.getData();
                            final String tooltipText = toolTipProvider.getToolTipText(element);

                            /*
                             * If the provider does not provide text for this
                             * element, bail out
                             */
                            if (tooltipText == null) {
                                return;
                            }

                            if (tip != null && !tip.isDisposed()) {
                                tip.dispose();
                            }
                            tip = new Shell(shell, SWT.ON_TOP | SWT.TOOL);
                            final FillLayout layout = new FillLayout();
                            layout.marginHeight = 5;
                            layout.marginWidth = 5;
                            tip.setLayout(layout);
                            tip.setBackground(shell.getDisplay().getSystemColor(SWT.COLOR_INFO_BACKGROUND));
                            label = new Label(tip, SWT.NONE);
                            label.setForeground(shell.getDisplay().getSystemColor(SWT.COLOR_INFO_FOREGROUND));
                            label.setBackground(shell.getDisplay().getSystemColor(SWT.COLOR_INFO_BACKGROUND));
                            label.setData("_TABLEITEM", item); //$NON-NLS-1$
                            label.setText(tooltipText);
                            label.addListener(SWT.MouseExit, labelListener);
                            label.addListener(SWT.MouseDown, labelListener);
                            final Point size = tip.computeSize(SWT.DEFAULT, SWT.DEFAULT);
                            final Point pt = table.toDisplay(event.x, event.y + 20);
                            tip.setBounds(pt.x, pt.y, size.x, size.y);
                            tip.setVisible(true);
                        }
                    }
                }
            }
        };
        table.addListener(SWT.Dispose, tableListener);
        table.addListener(SWT.KeyDown, tableListener);
        table.addListener(SWT.MouseMove, tableListener);
        table.addListener(SWT.MouseHover, tableListener);
    }
}
