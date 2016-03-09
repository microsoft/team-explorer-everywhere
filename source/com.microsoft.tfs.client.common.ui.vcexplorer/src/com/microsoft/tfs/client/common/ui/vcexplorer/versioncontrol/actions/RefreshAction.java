// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.vcexplorer.versioncontrol.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import com.microsoft.tfs.client.common.ui.vcexplorer.versioncontrol.VersionControlEditor;

public class RefreshAction extends TeamViewerAction {
    @Override
    public void doRun(final IAction action) {
        /*
         * If the active workbench part is the VCE, just refresh it.
         */
        final IWorkbenchPart activePart = getTargetPart();
        if (activePart != null && activePart instanceof VersionControlEditor) {
            ((VersionControlEditor) activePart).refresh(true);
            return;
        }

        final IWorkbenchWindow workbenchWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();

        if (workbenchWindow == null) {
            return;
        }

        final IWorkbenchPage activePage = workbenchWindow.getActivePage();

        if (activePage == null) {
            return;
        }

        final IEditorPart activeEditor = activePage.getActiveEditor();

        if (activeEditor != null && activeEditor instanceof VersionControlEditor) {
            ((VersionControlEditor) activeEditor).refresh(true);
            return;
        }
    }
}
