// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.ui.wizard.importwizard;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.wizard.IWizardPage;

import com.microsoft.tfs.client.common.framework.resources.command.ResourceChangingNonWorkspaceCommand;
import com.microsoft.tfs.client.eclipse.TFSEclipseClientPlugin;
import com.microsoft.tfs.client.eclipse.sync.SynchronizeSubscriber;
import com.microsoft.tfs.client.eclipse.ui.commands.vc.ImportProjectsCommand;
import com.microsoft.tfs.client.eclipse.ui.wizard.importwizard.support.ImportFolderCollection;
import com.microsoft.tfs.client.eclipse.ui.wizard.importwizard.support.ImportItemCollectionBase;
import com.microsoft.tfs.client.eclipse.ui.wizard.importwizard.support.ImportOptions;
import com.microsoft.tfs.core.clients.versioncontrol.SourceControlCapabilityFlags;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;

/**
 *
 *
 * @threadsafety unknown
 */
public class TfsImportWizard extends ImportWizard {
    public TfsImportWizard() {
        super(SourceControlCapabilityFlags.TFS);
    }

    @Override
    protected void addWizardPages() {
        addPage(new TfsImportWizardTreePage());
        addPage(new ImportWizardWorkspacePage());
        addPage(new TfsImportWizardConfirmationPage());
    }

    @Override
    public IWizardPage getNextPage(final IWizardPage page) {
        final IWizardPage nextConnectionPage = getNextConnectionPage();

        if (nextConnectionPage != null) {
            if (!nextConnectionPage.getName().equals(getSelectionPageName()) || !skipCrossSelectionPage()) {
                return nextConnectionPage;
            }
        }

        if (!hasPageData(Workspace.class)) {
            return getPage(ImportWizardWorkspacePage.PAGE_NAME);
        }

        if (!hasPageData(ImportItemCollectionBase.class)) {
            return getPage(TfsImportWizardTreePage.PAGE_NAME);
        }

        if (TfsImportWizardTreePage.PAGE_NAME.equals(page.getName())) {
            return getPage(TfsImportWizardConfirmationPage.PAGE_NAME);
        }

        return null;
    }

    private boolean skipCrossSelectionPage() {
        ImportOptions options = (ImportOptions) getPageData(ImportOptions.class);
        return options.getImportFolders().length > 0;
    }

    @Override
    protected boolean enableFinish(final IWizardPage currentPage) {
        if (TfsImportWizardConfirmationPage.PAGE_NAME.equals(currentPage.getName())) {
            return true;
        }

        return false;
    }

    @Override
    public boolean enableNext(final IWizardPage currentPage) {
        if (!enableNextConnectionPage(currentPage)) {
            return false;
        }

        if (TfsImportWizardConfirmationPage.PAGE_NAME.equals(currentPage.getName())) {
            return false;
        }

        return true;
    }

    @Override
    protected IStatus importProjects() {
        final ImportOptions options = (ImportOptions) getPageData(ImportOptions.class);
        final Workspace workspace = (Workspace) getPageData(Workspace.class);

        final ImportFolderCollection folderCollection =
            (ImportFolderCollection) getPageData(ImportItemCollectionBase.class);

        try {
            /*
             * Defer resource data manager auto-updates until we've finished the
             * entire import process. This is critical for local workspaces, as
             * we need to avoid a background job that could lock the metadata
             * files.
             */
            TFSEclipseClientPlugin.getDefault().getResourceDataManager().deferAutomaticRefresh();

            try {
                /*
                 * Defer synchronize auto-refreshes (based on core events)
                 * temporarily. We want to pend adds before we hook the project
                 * to the ProjectRepositoryManager. This would otherwise cause a
                 * race where we try to sync before the project is "hooked up".
                 */
                SynchronizeSubscriber.getInstance().deferAutomaticRefresh();

                /**
                 * Wrap the import command in a
                 * {@link ResourceChangingNonWorkspaceCommand}. This will notify
                 * listeners that they should ignore resource changes on the
                 * executing thread, however it will NOT take a workspace lock.
                 * This is necessary as various other components of the import
                 * wizard can take a workspace lock, which would lead to a
                 * deadlock.
                 */
                final ImportProjectsCommand importCommand =
                    new ImportProjectsCommand(workspace, folderCollection, options);
                return getCommandExecutor().execute(new ResourceChangingNonWorkspaceCommand(importCommand));
            } finally {
                SynchronizeSubscriber.getInstance().continueAutomaticRefresh();
            }
        } finally {
            TFSEclipseClientPlugin.getDefault().getResourceDataManager().continueAutomaticRefresh();
        }
    }

}
