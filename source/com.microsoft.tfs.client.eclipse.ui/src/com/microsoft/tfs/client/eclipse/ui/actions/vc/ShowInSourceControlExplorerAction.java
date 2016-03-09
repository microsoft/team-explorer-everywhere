// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.ui.actions.vc;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import com.microsoft.tfs.client.common.item.ServerItemPath;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.framework.action.ExtendedAction;
import com.microsoft.tfs.client.common.ui.vcexplorer.versioncontrol.VersionControlEditor;
import com.microsoft.tfs.client.common.ui.vcexplorer.versioncontrol.VersionControlEditorInput;
import com.microsoft.tfs.client.eclipse.resource.PluginResourceFilters;
import com.microsoft.tfs.client.eclipse.ui.actions.ActionHelpers;
import com.microsoft.tfs.core.clients.versioncontrol.WorkspaceLocation;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PathTranslation;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;

public class ShowInSourceControlExplorerAction extends ExtendedAction {
    @Override
    protected void onSelectionChanged(final IAction action, final ISelection selection) {
        if (action.isEnabled() == false) {
            return;
        }

        // We can only show items that are in the repository or a pending change
        if (!ActionHelpers.filterAcceptsAnyResource(
            selection,
            PluginResourceFilters.HAS_PENDING_CHANGES_OR_IN_REPOSITORY_FILTER)) {
            action.setEnabled(false);
            return;
        }

        // Get the selected resource.
        final IResource selectedResource = (IResource) adaptSelectionFirstElement(IResource.class);
        if (selectedResource == null) {
            action.setEnabled(false);
            return;
        }

        // Only FILE and FOLDER/PROJECT resources are handled.
        final int resourceType = selectedResource.getType();
        if (resourceType != IResource.FILE && resourceType != IResource.FOLDER && resourceType != IResource.PROJECT) {
            action.setEnabled(false);
            return;
        }

        final TFSRepository[] repositories = ActionHelpers.getRepositoriesFromSelection(getSelection());

        // Array will be empty if offline for a server workspace
        if (repositories.length != 1) {
            action.setEnabled(false);
            return;
        }

        // Local workspaces need to be "connected" to open SCE
        if (repositories[0].getWorkspace().getLocation() == WorkspaceLocation.LOCAL
            && repositories[0].getWorkspace().getClient().getConnection().getConnectivityFailureOnLastWebServiceCall()) {
            action.setEnabled(false);
            return;
        }

        action.setEnabled(true);
    }

    @Override
    public void doRun(final IAction action) {
        // Get the selected resource.
        final IResource selectedResource = (IResource) adaptSelectionFirstElement(IResource.class);
        if (selectedResource == null) {
            return;
        }

        // Find the current workspace.
        final TFSRepository repository =
            TFSCommonUIClientPlugin.getDefault().getProductPlugin().getRepositoryManager().getDefaultRepository();

        if (repository == null) {
            return;
        }

        // Translate the local path to a server path.
        final Workspace workspace = repository.getWorkspace();
        final PathTranslation translation =
            workspace.translateLocalPathToServerPath(selectedResource.getLocation().toOSString());

        if (translation == null) {
            return;
        }

        final String serverPath = translation.getTranslatedPath();
        if (serverPath == null) {
            return;
        }

        // open the source control editor.
        final IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();

        try {
            page.openEditor(new VersionControlEditorInput(), VersionControlEditor.ID);
            final VersionControlEditor editor = VersionControlEditor.getCurrent();

            if (editor != null) {
                switch (selectedResource.getType()) {
                    case IResource.FILE:
                        editor.setSelectedFile(new ServerItemPath(serverPath));
                        break;

                    case IResource.PROJECT:
                    case IResource.FOLDER:
                        editor.setSelectedFolder(new ServerItemPath(serverPath));
                        break;

                    default:
                        break;
                }
            }
        } catch (final PartInitException e) {
            throw new RuntimeException(e);
        }
    }
}
