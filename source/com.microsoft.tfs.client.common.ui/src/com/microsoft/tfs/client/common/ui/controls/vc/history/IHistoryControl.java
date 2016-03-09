// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.vc.history;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPartSite;

import com.microsoft.tfs.client.common.ui.controls.generic.compatibility.table.DoubleClickListener;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Changeset;

public interface IHistoryControl extends ISelectionProvider {
    @Override
    public void addSelectionChangedListener(ISelectionChangedListener listener);

    @Override
    public void removeSelectionChangedListener(ISelectionChangedListener listener);

    @Override
    public void setSelection(ISelection selection);

    public void refresh();

    public void setInput(HistoryInput input);

    public void addDoubleClickListener(DoubleClickListener listener);

    public IAction getCopyAction();

    public Changeset getSelectedChangeset();

    @Override
    public ISelection getSelection();

    public void addMenuListener(IMenuListener listener);

    public boolean setFocus();

    public Shell getShell();

    public void registerContextMenu(IWorkbenchPartSite site);

}
