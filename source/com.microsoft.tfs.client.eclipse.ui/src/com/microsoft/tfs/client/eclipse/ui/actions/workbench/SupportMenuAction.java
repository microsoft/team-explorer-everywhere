// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.ui.actions.workbench;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

import com.microsoft.tfs.client.common.ui.diagnostics.SupportUtils;

public class SupportMenuAction implements IWorkbenchWindowActionDelegate {
    private Shell shell;

    @Override
    public void dispose() {
    }

    @Override
    public void init(final IWorkbenchWindow window) {
        shell = window.getShell();
    }

    @Override
    public void run(final IAction action) {
        SupportUtils.openSupportDialog(shell, getClass().getClassLoader());
    }

    @Override
    public void selectionChanged(final IAction action, final ISelection selection) {
    }
}