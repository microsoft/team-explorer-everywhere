// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teambuild.controls;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import com.microsoft.tfs.client.common.ui.controls.generic.BaseControl;
import com.microsoft.tfs.client.common.ui.framework.image.ImageHelper;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.teambuild.TFSTeamBuildPlugin;

/**
 * A tab like interface to mimick the interface used in the Microsoft Build
 * Definition Editing screen. It consists of a table control on the left hand
 * side that shows the tabs available and a stack layout on the right hand side
 * that displays the pages.
 */
public class ToolStripTabs extends BaseControl {
    private Control[] pages;
    private Composite pane;
    private Table table;
    private final ToolStripTabPage[] tabs;
    private final ImageHelper imageHelper;
    private boolean valid;

    public ToolStripTabs(final Composite parent, final ToolStripTabPage[] tabs, final int style) {
        super(parent, style);
        this.tabs = tabs;
        imageHelper = new ImageHelper(TFSTeamBuildPlugin.PLUGIN_ID);
        createControls(this);
        setSelectedPage(0);
        addTraverseListener(new TraverseListener() {
            @Override
            public void keyTraversed(final TraverseEvent e) {
                if (e.detail == SWT.TRAVERSE_PAGE_NEXT) {
                    traverseToNextPage(true);
                    return;
                }
                if (e.detail == SWT.TRAVERSE_PAGE_PREVIOUS) {
                    traverseToNextPage(false);
                    return;
                }
            }
        });
    }

    private void traverseToNextPage(final boolean down) {
        final int count = table.getItemCount();
        if (count <= 1) {
            return;
        }
        int index = table.getSelectionIndex();
        if (index == -1) {
            index = 0;
        } else {
            final int offset = (down) ? 1 : -1;
            index = (index + offset + count) % count;
        }
        setSelectedPage(index);
    }

    private void createControls(final Composite composite) {
        final GridLayout layout = new GridLayout(2, false);
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        layout.horizontalSpacing = getHorizontalSpacing() * 2;
        layout.verticalSpacing = getVerticalSpacing();
        setLayout(layout);

        table = new Table(composite, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
        GridDataBuilder.newInstance().fill().applyTo(table);
        final TableColumn column = new TableColumn(table, SWT.NONE);

        /**
         * Make the column take up the full width. This looks the best on all
         * platforms especially Vista. Without this, you can get vertical lines
         * appearing or the selection does not go across the full column.
         */
        table.setLayout(new Layout() {
            @Override
            protected Point computeSize(
                final Composite composite,
                final int wHint,
                final int hHint,
                final boolean flushCache) {
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
                columns[0].setWidth(clientArea.width);
            }
        });

        pane = new Composite(this, SWT.NONE);
        GridDataBuilder.newInstance().fill().grab().applyTo(pane);

        final StackLayout stack = new StackLayout();
        pane.setLayout(stack);
        pages = new Control[tabs.length];
        for (int i = 0; i < tabs.length; i++) {
            final TableItem item = new TableItem(table, SWT.NONE);
            item.setText(0, tabs[i].getName());
            pages[i] = tabs[i].createControl(pane);
        }

        validate();

        table.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                setSelectedPage(table.getSelectionIndex());
                pane.setFocus();
                validate();
            }
        });
        // We have text and images now.
        column.pack();

        // TODO - CTRL+Page up/down should navigate between tabs.
        // composite.addKeyListener(new KeyAdapter()
        // {
        // public void keyPressed(KeyEvent e)
        // {
        // // if ((e.stateMask & SWT.MOD1) > 0 && e.keyCode == SWT.PAGE_UP)
        // // {
        // // System.out.println("CTRL + Page Up");
        // // return;
        // // }
        // // if ((e.stateMask & SWT.MOD1) > 0 && e.keyCode == SWT.PAGE_UP)
        // // {
        // // System.out.println("CTRL + Page Up");
        // // return;
        // // }
        // }
        // });
    }

    public Control setSelectedPage(final int index) {
        final Control control = pages[index];
        final StackLayout stack = (StackLayout) pane.getLayout();
        stack.topControl = control;
        table.select(index);
        pane.layout();
        return control;
    }

    public boolean validate() {
        boolean valid = true;
        for (int i = 0; i < tabs.length; i++) {
            if (tabs[i].isValid()) {
                table.getItem(i).setImage(imageHelper.getImage("icons/transparent16.gif")); //$NON-NLS-1$
            } else {
                table.getItem(i).setImage(imageHelper.getImage("icons/warning.gif")); //$NON-NLS-1$
                valid = false;
            }
        }
        this.valid = valid;
        return this.valid;
    }

    /**
     * @return true if all the pages are showing as valid, false if one or more
     *         are not.
     */
    public boolean isValid() {
        return valid;
    }

}
