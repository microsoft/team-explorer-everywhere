// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.ui.actions.vc;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.wizard.WizardDialog;

import com.microsoft.tfs.client.common.ui.vc.tfsitem.TFSFolder;
import com.microsoft.tfs.client.common.ui.vcexplorer.versioncontrol.actions.TeamViewerAction;
import com.microsoft.tfs.client.common.ui.vcexplorer.versioncontrol.actions.helper.ActionEnablementHelper;
import com.microsoft.tfs.client.eclipse.ui.wizard.importwizard.TfsImportWizard;

public class ImportFolderAction extends TeamViewerAction {
    private TFSFolder[] items;

    @Override
    public void doRun(final IAction action) {
        if (items == null) {
            return;
        }

        final TfsImportWizard importWizard = new TfsImportWizard();

        // Add current selected folders as selection.
        final String[] importFolders = new String[items.length];
        for (int i = 0; i < importFolders.length; i++) {
            importFolders[i] = items[i].getFullPath();
        }
        importWizard.setImportFolders(importFolders);

        importWizard.init(getWorkbenchWindow().getWorkbench(), null);

        final WizardDialog dialog = new WizardDialog(getShell(), importWizard);
        dialog.open();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onSelectionChanged(final IAction action, final ISelection selection) {
        if (action.isEnabled() == false) {
            return;
        }

        items = (TFSFolder[]) adaptSelectionToArray(TFSFolder.class);

        action.setEnabled(ActionEnablementHelper.selectionContainsRoot(selection) == false
            && ActionEnablementHelper.selectionContainsProjectFolder(selection) == false
            && ActionEnablementHelper.selectionContainsDeletedFolder(selection) == false);
    }

}
