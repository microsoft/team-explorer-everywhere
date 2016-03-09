// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.ui.wizard.importwizard;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import com.microsoft.tfs.client.common.codemarker.CodeMarker;
import com.microsoft.tfs.client.common.codemarker.CodeMarkerDispatch;
import com.microsoft.tfs.client.common.git.utils.GitHelpers;
import com.microsoft.tfs.client.common.repository.RepositoryManager.RepositoryStatus;
import com.microsoft.tfs.client.common.repository.RepositoryManager.RepositoryStatusContainer;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.server.TFSServer;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.commands.GetDefaultWorkspaceCommand;
import com.microsoft.tfs.client.common.ui.framework.image.ImageHelper;
import com.microsoft.tfs.client.common.ui.teamexplorer.pages.TeamExplorerPendingChangesPage;
import com.microsoft.tfs.client.common.ui.views.TeamExplorerView;
import com.microsoft.tfs.client.common.ui.wizard.connectwizard.ConnectWizard;
import com.microsoft.tfs.client.eclipse.TFSEclipseClientPlugin;
import com.microsoft.tfs.client.eclipse.ui.Messages;
import com.microsoft.tfs.client.eclipse.ui.TFSEclipseClientUIPlugin;
import com.microsoft.tfs.client.eclipse.ui.wizard.connectwizard.EclipseConnectWizard;
import com.microsoft.tfs.client.eclipse.ui.wizard.importwizard.support.ImportOptions;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.commonstructure.CommonStructureClient;
import com.microsoft.tfs.core.clients.commonstructure.ProjectInfo;
import com.microsoft.tfs.core.clients.versioncontrol.SourceControlCapabilityFlags;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;

public abstract class ImportWizard extends EclipseConnectWizard implements IImportWizard {
    private static final Log logger = LogFactory.getLog(ImportWizard.class);
    public static final CodeMarker CODEMARKER_IMPORT_COMPLETE =
        new CodeMarker("com.microsoft.tfs.client.eclipse.ui.wizard.importwizard.ImportWizard#importComplete"); //$NON-NLS-1$

    private static final ImageHelper imageHelper = new ImageHelper(TFSEclipseClientUIPlugin.PLUGIN_ID);

    private final ImportOptions options = new ImportOptions(new ImportWizardNewProjectAction());

    public ImportWizard(final SourceControlCapabilityFlags capabilityFlags) {
        super(
            Messages.getString("ImportWizard.WizardTitle"), //$NON-NLS-1$
            Messages.getString("ImportWizard.WizardDescription"), //$NON-NLS-1$
            imageHelper.getImageDescriptor(TFSCommonUIClientPlugin.PLUGIN_ID, "images/wizard/pageheader.png"), //$NON-NLS-1$
            availableSourceControl(capabilityFlags),
            capabilityFlags == SourceControlCapabilityFlags.TFS ? ConnectWizard.PROJECT_SELECTION
                : ConnectWizard.SERVERONLY_SELECTION);

        logger.info("Import wizard starting"); //$NON-NLS-1$

        options.setCapabilityFlags(getSourceControlCapabilityFlags());

        addConnectionPages();
        addWizardPages();
    }

    /**
     * Specific import wizards, i.e. GitImportWizard or TfsImportWizard, add
     * their own set of pages
     *
     */
    protected void addWizardPages() {
    }

    @Override
    public void init(final IWorkbench workbench, final IStructuredSelection selection) {
        /* Set up ImportWizardOptions */
        options.setEclipseWorkspace(ResourcesPlugin.getWorkspace());
        setPageData(ImportOptions.class, options);
        setPageData(IWorkbench.class, workbench);

        /* Initialize connection pages */
        final TFSRepository repository = initConnectionPages();

        if (repository != null) {
            options.setTFSWorkspace(repository.getWorkspace());
        }
    }

    public void setImportFolders(final String[] serverPaths) {
        options.setImportFolders(serverPaths);
    }

    @Override
    protected void setRepository(final TFSRepository repository) {
        if (hasPageData(Workspace.class)) {
            options.setTFSWorkspace((Workspace) getPageData(Workspace.class));
        } else {
            options.setTFSWorkspace(null);
        }
    }

    public List<ProjectInfo> getInitialTeamProjectList() {
        final ProjectInfo[] selectedProjects;

        try {
            final TFSTeamProjectCollection connection =
                (TFSTeamProjectCollection) getPageData(TFSTeamProjectCollection.class);
            final CommonStructureClient client =
                (CommonStructureClient) connection.getClient(CommonStructureClient.class);
            selectedProjects = client.listAllProjects();
        } catch (final Exception e) {
            logger.error("Error getting initial team projects", e); //$NON-NLS-1$
            return new ArrayList<ProjectInfo>();
        }

        final List<ProjectInfo> filteredProjects = new ArrayList<ProjectInfo>();
        for (final ProjectInfo project : selectedProjects) {
            if (project.getSourceControlCapabilityFlags().contains(options.getCapabilityFlags())) {
                filteredProjects.add(project);
            }
        }

        return filteredProjects;
    }

    @Override
    protected boolean doPerformFinish() {
        logger.info("Import wizard is finishing"); //$NON-NLS-1$

        final TFSTeamProjectCollection connection =
            (TFSTeamProjectCollection) getPageData(TFSTeamProjectCollection.class);

        final Workspace workspace;
        if (hasPageData(Workspace.class) && getPageData(Workspace.class) != null) {
            workspace = (Workspace) getPageData(Workspace.class);
        } else {
            /* If the user didn't select a workspace, get the default. */
            final GetDefaultWorkspaceCommand workspaceCommand = new GetDefaultWorkspaceCommand(connection);
            final IStatus workspaceStatus = getCommandExecutor().execute(workspaceCommand);

            if (!workspaceStatus.isOK()) {
                return false;
            }

            workspace = workspaceCommand.getWorkspace();
            setPageData(Workspace.class, workspace);
        }

        /* Setup a Repository */
        final RepositoryStatusContainer repositoryStatus = new RepositoryStatusContainer();
        final TFSServer server = TFSEclipseClientPlugin.getDefault().getServerManager().getOrCreateServer(connection);
        final TFSRepository repository =
            TFSEclipseClientPlugin.getDefault().getRepositoryManager().getOrCreateRepository(
                workspace,
                repositoryStatus);

        options.setTFSRepository(repository);

        final IStatus importStatus = importProjects();

        if (importStatus.getSeverity() >= IStatus.ERROR) {
            /*
             * On failure, unhook the TFS Repository if we configured it AND if
             * all projects failed to import (ie, no other projects are using
             * the repository.) This allows users to reconfigure the connection.
             */
            if (repositoryStatus.getRepositoryStatus().equals(RepositoryStatus.CREATED)
                && TFSEclipseClientPlugin.getDefault().getProjectManager().getProjectsForRepository(
                    repository).length == 0) {
                repository.close();
                server.close();

                TFSEclipseClientPlugin.getDefault().getRepositoryManager().removeRepository(repository);
                TFSEclipseClientPlugin.getDefault().getServerManager().removeServer(server);
                return false;
            }
        }

        /*
         * Set up the connection now that we have one. Don't set it up before
         * because failures above will cause us to keep our connection open
         * despite not having anything imported.
         */
        finishConnection();

        finishWorkspace(workspace);

        /* Open the Pending Changes View */
        IWorkbench workbench = null;

        if (hasPageData(IWorkbench.class)) {
            workbench = (IWorkbench) getPageData(IWorkbench.class);
        } else {
            try {
                workbench = PlatformUI.getWorkbench();
            } catch (final Exception e) {
                logger.info("Could not get workbench", e); //$NON-NLS-1$
            }
        }

        if (workbench != null) {
            try {
                // Ensure the Team Explorer View is shown.
                final TeamExplorerView teamExplorerView =
                    (TeamExplorerView) workbench.getActiveWorkbenchWindow().getActivePage().showView(
                        TeamExplorerView.ID);

                // Navigate to pending changes page only in case of TE is
                // connected to
                // a TFS project and import from TFS
                if (teamExplorerView.getContext().getSourceControlCapability().contains(
                    SourceControlCapabilityFlags.TFS)
                    && getSourceControlCapabilityFlags().contains(SourceControlCapabilityFlags.TFS)) {
                    // Ensure the pending changes page is shown in Team
                    // Explorer.
                    teamExplorerView.navigateToPageID(TeamExplorerPendingChangesPage.ID);
                }
            } catch (final PartInitException e) {
                logger.warn("Could not open Team Explorer View", e); //$NON-NLS-1$
            }
        }

        CodeMarkerDispatch.dispatch(CODEMARKER_IMPORT_COMPLETE);
        return true;
    }

    protected abstract IStatus importProjects();

    @Override
    protected boolean doPerformCancel() {
        logger.info("Import wizard cancelled"); //$NON-NLS-1$

        return true;
    }

    private static SourceControlCapabilityFlags availableSourceControl(
        final SourceControlCapabilityFlags capabilityFlags) {
        SourceControlCapabilityFlags flags = capabilityFlags;
        if (capabilityFlags.contains(SourceControlCapabilityFlags.GIT)) {
            if (!GitHelpers.isEGitInstalled(true)) {
                flags = flags.remove(SourceControlCapabilityFlags.GIT);
            }
        }

        return flags;
    }
}
