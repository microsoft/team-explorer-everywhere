// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.vc.history;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbenchPartSite;

import com.microsoft.tfs.client.common.ui.controls.generic.compatibility.table.DoubleClickListener;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Changeset;

public class HistoryCombinedControl extends Composite implements IHistoryControl {

    private boolean singleItem = true;

    private final HistoryCachedTableControl tableControl;

    private final HistoryTreeControl treeControl;

    private final StackLayout stack = new StackLayout();

    public HistoryCombinedControl(final Composite parent, final int style) {
        super(parent, style);
        setLayout(stack);

        /*
         * table controls always force border on (meh.) turn border on for the
         * tree control to matchy match.
         */
        tableControl = new HistoryCachedTableControl(this, style);
        treeControl = new HistoryTreeControl(this, style | SWT.BORDER);
        stack.topControl = treeControl;
    }

    @Override
    public void addSelectionChangedListener(final ISelectionChangedListener listener) {
        tableControl.addSelectionChangedListener(listener);
        treeControl.addSelectionChangedListener(listener);
    }

    @Override
    public void removeSelectionChangedListener(final ISelectionChangedListener listener) {
        tableControl.removeSelectionChangedListener(listener);
        treeControl.removeSelectionChangedListener(listener);
    }

    @Override
    public void setSelection(final ISelection selection) {
        tableControl.setSelection(selection);
        treeControl.setSelection(selection);
    }

    @Override
    public void refresh() {
        tableControl.refresh();
        treeControl.refresh();
    }

    @Override
    public void setInput(final HistoryInput input) {
        if (input == null) {
            treeControl.setInput(null);
            tableControl.setInput(null);
            return;
        }
        singleItem = input.isSingleItem();
        if (singleItem) {
            tableControl.setInput(null);
            treeControl.setInput(input);
            stack.topControl = treeControl;
            this.layout();
        } else {
            treeControl.setInput(null);
            tableControl.setInput(input);
            stack.topControl = tableControl;
            this.layout();
        }
    }

    @Override
    public void addDoubleClickListener(final DoubleClickListener listener) {
        tableControl.addDoubleClickListener(listener);
        treeControl.addDoubleClickListener(listener);
    }

    @Override
    public IAction getCopyAction() {
        if (singleItem) {
            return null;
        } else {
            return tableControl.getCopyAction();
        }
    }

    @Override
    public Changeset getSelectedChangeset() {
        if (singleItem) {
            return treeControl.getSelectedChangeset();
        } else {
            return tableControl.getSelectedChangeset();
        }
    }

    @Override
    public ISelection getSelection() {
        if (singleItem) {
            return treeControl.getSelection();
        } else {
            return tableControl.getSelection();
        }
    }

    @Override
    public void addMenuListener(final IMenuListener listener) {
        tableControl.addMenuListener(listener);
        treeControl.addMenuListener(listener);
    }

    @Override
    public void registerContextMenu(final IWorkbenchPartSite site) {
        tableControl.registerContextMenu(site);
        treeControl.registerContextMenu(site);
    }
}
