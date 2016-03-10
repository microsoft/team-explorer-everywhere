// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.ui.egit.importwizard;

import java.util.List;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.ui.IWorkingSet;

import com.microsoft.tfs.client.common.framework.resources.command.ResourceChangingNonWorkspaceCommand;
import com.microsoft.tfs.client.common.git.EclipseProjectInfo;
import com.microsoft.tfs.client.common.git.utils.GitHelpers;
import com.microsoft.tfs.client.common.ui.framework.helper.MessageBoxHelpers;
import com.microsoft.tfs.client.common.ui.framework.helper.UIHelpers;
import com.microsoft.tfs.client.common.ui.vc.serveritem.TypedServerGitRepository;
import com.microsoft.tfs.client.common.ui.vc.serveritem.TypedServerItem;
import com.microsoft.tfs.client.common.ui.wizard.common.WizardCrossCollectionSelectionPage;
import com.microsoft.tfs.client.eclipse.ui.egit.Messages;
import com.microsoft.tfs.client.eclipse.ui.wizard.importwizard.ImportWizard;
import com.microsoft.tfs.client.eclipse.ui.wizard.importwizard.ImportWizardWorkspacePage;
import com.microsoft.tfs.client.eclipse.ui.wizard.importwizard.support.ImportOptions;
import com.microsoft.tfs.core.clients.versioncontrol.SourceControlCapabilityFlags;

/**
 *
 *
 * @threadsafety unknown
 */
public class GitImportWizard extends ImportWizard {
    public GitImportWizard() {
        this(null);
    }

    public GitImportWizard(final List<TypedServerItem> initialSelectedItems) {
        super(SourceControlCapabilityFlags.GIT);
        if (initialSelectedItems != null && initialSelectedItems.size() > 0) {
            // There should only be one repo selected, so take the first one and
            // add it
            if (initialSelectedItems.get(0) instanceof TypedServerGitRepository) {
                setPageData(
                    WizardCrossCollectionRepoSelectionPage.INITIALLY_SELECTED_REPO,
                    (TypedServerGitRepository) initialSelectedItems.get(0));
            }
        }
    }

    @Override
    protected void addWizardPages() {

        GitHelpers.activateEGitUI();

        addPage(new ImportWizardWorkspacePage());
        addPage(new GitImportWizardSelectFoldersPage());
        addPage(new GitImportWizardSelectProjectsPage());
    }

    @Override
    protected WizardCrossCollectionSelectionPage getSelectionPage() {
        final WizardCrossCollectionSelectionPage selectionPage;
        selectionPage = new WizardCrossCollectionRepoSelectionPage();
        return selectionPage;
    }

    @Override
    public IWizardPage getNextPage(final IWizardPage page) {
        if (getSourceControlCapabilityFlags().isEmpty()) {
            MessageBoxHelpers.warningMessageBox(
                getShell(),
                Messages.getString("GitImportWizard.EGitRequiredTitle"), //$NON-NLS-1$
                Messages.getString("GitImportWizard.EGitRequiredWarning")); //$NON-NLS-1$
            return null;
        }

        final IWizardPage nextConnectionPage = getNextConnectionPage();

        if (nextConnectionPage != null) {
            return nextConnectionPage;
        }

        // The new flow reduces the repo selection down to one page
        if (!hasPageData(GitImportWizardSelectFoldersPage.SELECTED_FOLDERS)) {
            return getPage(GitImportWizardSelectFoldersPage.PAGE_NAME);
        }

        if (GitImportWizardSelectFoldersPage.PAGE_NAME.equals(page.getName())) {
            return getPage(GitImportWizardSelectProjectsPage.PAGE_NAME);
        }

        return null;
    }

    @Override
    protected boolean enableFinish(final IWizardPage currentPage) {
        if (GitImportWizardSelectProjectsPage.PAGE_NAME.equals(currentPage.getName())) {
            return true;
        }

        return false;
    }

    @Override
    public boolean enableNext(final IWizardPage currentPage) {
        if (!enableNextConnectionPage(currentPage)) {
            return false;
        }

        if (GitImportWizardSelectProjectsPage.PAGE_NAME.equals(currentPage.getName())) {
            return false;
        }

        return true;
    }

    @Override
    protected IStatus importProjects() {
        final ImportOptions options = (ImportOptions) getPageData(ImportOptions.class);

        if (options.isUseNewProjectWizard()) {
            UIHelpers.asyncExec(new Runnable() {
                @Override
                public void run() {
                    options.getNewProjectAction().run();
                }
            });
        } else if (hasPageData(EclipseProjectInfo.class)) {
            final EclipseProjectInfo[] projects = (EclipseProjectInfo[]) getPageData(EclipseProjectInfo.class);

            if (projects.length > 0) {
                final IWorkspace workspace = options.getEclipseWorkspace();
                final IWorkingSet workingSet = options.getWorkingSet();

                final ImportEclipseProjectsCommand importCommand =
                    new ImportEclipseProjectsCommand(workspace, projects, workingSet);

                return getCommandExecutor().execute(new ResourceChangingNonWorkspaceCommand(importCommand));
            }
        }

        return Status.OK_STATUS;
    }

}
