// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teambuild.teamexplorer.actions;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import com.microsoft.tfs.client.common.item.ServerItemPath;
import com.microsoft.tfs.client.common.ui.vcexplorer.versioncontrol.VersionControlEditor;
import com.microsoft.tfs.client.common.ui.vcexplorer.versioncontrol.VersionControlEditorInput;
import com.microsoft.tfs.core.clients.build.BuildSourceProviders;
import com.microsoft.tfs.core.clients.build.IBuildDefinition;
import com.microsoft.tfs.core.clients.build.IBuildDefinitionSourceProvider;
import com.microsoft.tfs.util.StringUtil;

public class OpenBuildConfigurationFolderAction extends TeamExplorerSingleBuildDefinitionAction {
    private static final Log log = LogFactory.getLog(OpenBuildConfigurationFolderAction.class);

    @Override
    protected void onSelectionChanged(final IAction action, final ISelection selection) {
        super.onSelectionChanged(action, selection);

        if (!action.isEnabled()) {
            return;
        }

        action.setEnabled(isTfs(selectedDefinition));
    }

    private boolean isTfs(final IBuildDefinition definition) {
        if (definition.getSourceProviders().length == 0) {
            return true;
        }

        final IBuildDefinitionSourceProvider sourceProvider = definition.getSourceProviders()[0];

        if (BuildSourceProviders.isTfGit(sourceProvider)) {
            return false;
        }

        return true;
    }

    @Override
    public void doRun(final IAction action) {
        if (selectedDefinition == null) {
            return;
        }

        final String configurationPath = selectedDefinition.getConfigurationFolderPath();

        if (StringUtil.isNullOrEmpty(configurationPath)) {
            return;
        }

        openSourceControlExplorer(configurationPath);
    }

    public static void openSourceControlExplorer(final String path) {
        final IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
        try {
            page.openEditor(new VersionControlEditorInput(), VersionControlEditor.ID);
            if (VersionControlEditor.getCurrent() != null) {
                VersionControlEditor.getCurrent().setSelectedFolder(new ServerItemPath(path));
            }
        } catch (final PartInitException e) {
            log.warn("Could not open version control editor", e); //$NON-NLS-1$
        }
    }
}
