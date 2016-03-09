// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.ui.actions.vc;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.PlatformUI;

import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.framework.action.ExtendedAction;
import com.microsoft.tfs.client.common.ui.framework.helper.MessageBoxHelpers;
import com.microsoft.tfs.client.common.ui.tasks.vc.RollbackTask;
import com.microsoft.tfs.client.common.ui.vc.serveritem.TypedServerItem;
import com.microsoft.tfs.client.eclipse.resource.PluginResourceFilters;
import com.microsoft.tfs.client.eclipse.ui.Messages;
import com.microsoft.tfs.client.eclipse.ui.TFSEclipseClientUIPlugin;
import com.microsoft.tfs.client.eclipse.ui.actions.ActionHelpers;
import com.microsoft.tfs.client.eclipse.ui.actions.AdaptedSelectionInfo;
import com.microsoft.tfs.core.clients.versioncontrol.WebServiceLevel;

public class RollbackAction extends ExtendedAction {
    @SuppressWarnings("unused")
    private static final Log log = LogFactory.getLog(RollbackAction.class);

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onSelectionChanged(final IAction action, final ISelection selection) {
        if (action.isEnabled() == false) {
            return;
        }

        final TFSRepository repository =
            TFSEclipseClientUIPlugin.getDefault().getRepositoryManager().getDefaultRepository();
        // Not allowed for TFS versions < 2010
        if (repository == null
            || repository.getVersionControlClient().getServiceLevel().getValue() < WebServiceLevel.TFS_2010.getValue()) {
            action.setEnabled(false);
            return;
        }

        action.setEnabled(
            ActionHelpers.filterAcceptsAnyResource(getSelection(), PluginResourceFilters.IN_REPOSITORY_FILTER));
    }

    @Override
    public void doRun(final IAction action) {
        final AdaptedSelectionInfo selectionInfo = ActionHelpers.adaptSelectionToStandardResources(
            getSelection(),
            PluginResourceFilters.IN_REPOSITORY_FILTER,
            false);

        if (!ActionHelpers.ensureNonZeroResourceCountAndSingleRepository(selectionInfo, getShell())) {
            return;
        }

        if (!PlatformUI.getWorkbench().saveAllEditors(true)) {
            return;
        }

        final IResource[] resources = selectionInfo.getResources();
        final TFSRepository repository = selectionInfo.getRepositories()[0];

        final TypedServerItem[] serverItems = TypedServerItem.getTypedServerItemFromResource(repository, resources);

        if (serverItems.length != 1) {
            MessageBoxHelpers.messageBox(
                getShell(),
                Messages.getString("RollbackAction.SelectionErrorTitle"), //$NON-NLS-1$
                Messages.getString("RollbackAction.SelectionErrorMessage")); //$NON-NLS-1$
            return;
        }

        final RollbackTask rollbackTask = new RollbackTask(getShell(), repository, serverItems[0].getServerPath());
        rollbackTask.run();
    }
}
