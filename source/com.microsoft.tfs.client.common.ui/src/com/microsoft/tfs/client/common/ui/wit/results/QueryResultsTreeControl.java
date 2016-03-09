// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.wit.results;

import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Tree;

import com.microsoft.tfs.client.common.ui.controls.generic.BaseControl;
import com.microsoft.tfs.client.common.ui.controls.generic.compatibility.table.CompatibilityVirtualTable;
import com.microsoft.tfs.client.common.ui.framework.sizing.MeasureItemHeightListener;

public class QueryResultsTreeControl extends BaseControl {

    // TODO - This will be the tree version of the control.

    /**
     * SWT styles that will always be used.
     */
    private static int TREE_STYLES = SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL | SWT.VIRTUAL | SWT.FULL_SELECTION;

    /**
     * SWT styles that will only be used if the client passes them in.
     */
    private static int OPTIONAL_TREE_STYLES = SWT.MULTI | SWT.CHECK;

    /**
     * The SWT style bits this {@link CompatibilityVirtualTable}'s {@link Table}
     * was created with.
     */
    private final int tableStyles;

    private final TreeViewer treeViewer;

    public QueryResultsTreeControl(final Composite parent, final int style) {
        super(parent, style);
        setLayout(new FillLayout());

        tableStyles = TREE_STYLES | (OPTIONAL_TREE_STYLES & style);

        treeViewer = new TreeViewer(this, tableStyles);
        setupTree(treeViewer.getTree());
    }

    private void setupTree(final Tree tree) {
        tree.setLinesVisible(true);
        tree.setHeaderVisible(true);
        tree.setLayout(new TreeLayout());

        /* Pad table height by four pixels to increase readability */
        tree.addListener(/* SWT.MeasureItem */41, new MeasureItemHeightListener(4));
        tree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseDown(final MouseEvent e) {
            }
        });
    }

    private class TreeLayout extends Layout {

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
            composite.getClientArea();

            System.out.println("Layout" + composite); //$NON-NLS-1$
        }
    }

}
