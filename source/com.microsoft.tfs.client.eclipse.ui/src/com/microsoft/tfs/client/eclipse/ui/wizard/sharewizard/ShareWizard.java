// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.ui.wizard.sharewizard;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.wizard.IWizardContainer;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.ui.IConfigurationWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import com.microsoft.tfs.client.common.codemarker.CodeMarker;
import com.microsoft.tfs.client.common.codemarker.CodeMarkerDispatch;
import com.microsoft.tfs.client.common.commands.vc.QueryItemsCommand;
import com.microsoft.tfs.client.common.commands.vc.ScanLocalWorkspaceCommand;
import com.microsoft.tfs.client.common.repository.RepositoryManager.RepositoryStatus;
import com.microsoft.tfs.client.common.repository.RepositoryManager.RepositoryStatusContainer;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.server.TFSServer;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.framework.command.UICommandExecutorFactory;
import com.microsoft.tfs.client.common.ui.framework.helper.ShellUtils;
import com.microsoft.tfs.client.common.ui.framework.image.ImageHelper;
import com.microsoft.tfs.client.common.ui.teamexplorer.pages.TeamExplorerPendingChangesPage;
import com.microsoft.tfs.client.common.ui.views.TeamExplorerView;
import com.microsoft.tfs.client.common.ui.wizard.connectwizard.ConnectWizard;
import com.microsoft.tfs.client.eclipse.TFSEclipseClientPlugin;
import com.microsoft.tfs.client.eclipse.commands.eclipse.ShareProjectsCommand;
import com.microsoft.tfs.client.eclipse.commands.eclipse.share.ShareProjectAction;
import com.microsoft.tfs.client.eclipse.commands.eclipse.share.ShareProjectConfiguration;
import com.microsoft.tfs.client.eclipse.sync.SynchronizeSubscriber;
import com.microsoft.tfs.client.eclipse.ui.Messages;
import com.microsoft.tfs.client.eclipse.ui.TFSEclipseClientUIPlugin;
import com.microsoft.tfs.client.eclipse.ui.wizard.connectwizard.EclipseConnectWizard;
import com.microsoft.tfs.client.eclipse.util.TeamUtils;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.versioncontrol.GetItemsOptions;
import com.microsoft.tfs.core.clients.versioncontrol.SourceControlCapabilityFlags;
import com.microsoft.tfs.core.clients.versioncontrol.WorkspaceLocation;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.DeletedState;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemSet;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.WorkingFolder;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.WorkspaceVersionSpec;
import com.microsoft.tfs.util.Check;

public class ShareWizard extends EclipseConnectWizard implements IConfigurationWizard, IAdaptable {
    private static final Log logger = LogFactory.getLog(ShareWizard.class);

    private final static ImageHelper imageHelper = new ImageHelper(TFSEclipseClientUIPlugin.PLUGIN_ID);

    public static final CodeMarker CODEMARKER_SHARE_COMPLETE =
        new CodeMarker("com.microsoft.tfs.client.eclipse.ui.wizard.sharewizard.ShareWizard#shareComplete"); //$NON-NLS-1$
    public static final CodeMarker CODEMARKER_QUERY_STARTED =
        new CodeMarker("com.microsoft.tfs.client.eclipse.ui.wizard.sharewizard.ShareWizard#queryStarted"); //$NON-NLS-1$
    public static final CodeMarker CODEMARKER_QUERY_COMPLETE =
        new CodeMarker("com.microsoft.tfs.client.eclipse.ui.wizard.sharewizard.ShareWizard#queryComplete"); //$NON-NLS-1$

    public ShareWizard() {
        super(
            Messages.getString("ShareWizard.WizardName"), //$NON-NLS-1$
            Messages.getString("ShareWizard.WizardDescription"), //$NON-NLS-1$
            imageHelper.getImageDescriptor(TFSCommonUIClientPlugin.PLUGIN_ID, "images/wizard/pageheader.png"), //$NON-NLS-1$
            SourceControlCapabilityFlags.TFS,
            ConnectWizard.PROJECT_SELECTION);

        addConnectionPages();
        addPage(new ShareWizardWorkspacePage());
        addPage(new ShareWizardWorkingFolderErrorPage());
        addPage(new ShareWizardTreePage());
        addPage(new ShareWizardSingleProjectConfirmationPage());
        addPage(new ShareWizardMultipleProjectConfirmationPage());
    }

    @Override
    public void init(final IWorkbench workbench, final IProject project) {
        Check.notNull(project, "project"); //$NON-NLS-1$

        initInternal(workbench, new IProject[] {
            project
        });
    }

    /*
     * This is package-protected so that {@link ShareWizardExtension} can access
     * this method.
     */
    void initInternal(final IWorkbench workbench, final IProject[] projects) {
        Check.notNullOrEmpty(projects, "projects"); //$NON-NLS-1$

        setPageData(IWorkbench.class, workbench);
        setPageData(IProject.class, projects);

        /* Initialize connection pages */
        final TFSRepository repository = initConnectionPages();

        if (repository != null) {
            setRepository(repository);
        }
    }

    @Override
    protected void setRepository(final TFSRepository repository) {
        final Workspace workspace = repository.getWorkspace();
        final Map<IProject, String> projectWorkingFolderMap = createProjectWorkingFolderMap(workspace);

        setPageData(ShareWizardWorkspacePage.PROJECT_WORKING_FOLDER_MAP, projectWorkingFolderMap);
    }

    @Override
    public boolean enableNext(final IWizardPage currentPage) {
        if (!enableNextConnectionPage(currentPage)) {
            return false;
        }

        if (ShareWizardSingleProjectConfirmationPage.PAGE_NAME.equals(currentPage.getName())) {
            return false;
        }

        if (ShareWizardMultipleProjectConfirmationPage.PAGE_NAME.equals(currentPage.getName())) {
            return false;
        }

        return true;
    }

    /*
     * We need to run some queries against the server to get the next page if
     * the next page is the confirmation page (ie, we are already connected and
     * license checks are okay.) However, we do not want to run this query the
     * first time the share wizard is opened, or it will run this before the
     * share wizard page is even drawn (and before the user has even selected us
     * as the share target!) Thus we override here to simply return a generic
     * page.
     */
    @Override
    public IWizardPage getStartingPage() {
        return getNextPage(null);
    }

    @Override
    public IWizardPage getNextPage(final IWizardPage page) {
        final IWizardPage nextConnectionPage = getNextConnectionPage();

        if (nextConnectionPage != null) {
            return nextConnectionPage;
        }

        if (!hasPageData(Workspace.class) || !hasPageData(ShareWizardWorkspacePage.PROJECT_WORKING_FOLDER_MAP)) {
            return getPage(ShareWizardWorkspacePage.PAGE_NAME);
        }

        /*
         * Determine what we're doing with these projects: connecting or
         * uploading
         */
        final ShareProjectAction action = getAction();

        /* Some projects have workfolds, some do not: error */
        if (action == null) {
            return getPage(ShareWizardWorkingFolderErrorPage.PAGE_NAME);
        }

        /* We need working folder mappings for these projects */
        if (action == ShareProjectAction.MAP_AND_UPLOAD && !hasPageData(ShareWizardTreePage.SERVER_PATHS)) {
            return getPage(ShareWizardTreePage.PAGE_NAME);
        }

        /* Build the configuration */
        if (!buildConfiguration()) {
            return null;
        }

        final IProject[] projects = (IProject[]) getPageData(IProject.class);

        if (projects.length == 1) {
            return getPage(ShareWizardSingleProjectConfirmationPage.PAGE_NAME);
        } else {
            return getPage(ShareWizardMultipleProjectConfirmationPage.PAGE_NAME);
        }
    }

    /**
     * Determine the action to be performed at the end of this wizard. If all
     * projects have working folder mappings and we merely need to connect them,
     * we return {@link ShareProjectAction#CONNECT}. If no projects have a
     * working folder mapping and we need to upload them, we return
     * {@link ShareProjectAction#MAP_AND_UPLOAD}. If some projects have working
     * folder mappings and some do not, we return null -- this wizard cannot
     * currently handle a mix of mapped and unmapped projects.
     *
     * @return A ShareProjectAction which describes the action to be performed
     *         by the wizard.
     */
    public ShareProjectAction getAction() {
        final IProject[] projects = (IProject[]) getPageData(IProject.class);
        final Map<IProject, String> projectWorkingFolderMap =
            (Map<IProject, String>) getPageData(ShareWizardWorkspacePage.PROJECT_WORKING_FOLDER_MAP);

        if (projects == null || projectWorkingFolderMap == null) {
            return null;
        }

        /* No projects have workfolds, upload them all. */
        if (projectWorkingFolderMap.size() == 0) {
            return ShareProjectAction.MAP_AND_UPLOAD;
        }

        /* All projects have workfolds, investigate further. */
        if (projects.length == projectWorkingFolderMap.size()) {
            final Workspace workspace = (Workspace) getPageData(Workspace.class);
            final ItemSpec[] itemSpecs = new ItemSpec[projects.length];

            for (int i = 0; i < projects.length; i++) {
                itemSpecs[i] = new ItemSpec(projects[i].getLocation().toOSString(), RecursionType.NONE);
            }

            final QueryItemsCommand queryCommand = new QueryItemsCommand(
                workspace.getClient(),
                itemSpecs,
                new WorkspaceVersionSpec(workspace),
                DeletedState.NON_DELETED,
                ItemType.ANY,
                GetItemsOptions.NONE);

            final IWizardContainer container = getContainer();

            Shell shell = getShell();

            if (shell == null) {
                shell = ShellUtils.getWorkbenchShell();
            }

            CodeMarkerDispatch.dispatch(CODEMARKER_QUERY_STARTED);

            IStatus queryStatus;

            if (container == null) {
                queryStatus = UICommandExecutorFactory.newUICommandExecutor(shell).execute(queryCommand);
            } else {
                queryStatus = UICommandExecutorFactory.newWizardCommandExecutor(getContainer()).execute(queryCommand);
            }

            CodeMarkerDispatch.dispatch(CODEMARKER_QUERY_COMPLETE);

            if (!queryStatus.isOK()) {
                return null;
            }

            final ItemSet[] itemSets = queryCommand.getItemSets();
            ShareProjectAction action = null;

            for (int i = 0; i < itemSpecs.length; i++) {
                final ShareProjectAction thisAction;

                /*
                 * Nothing exists on the server. This project needs to be
                 * uploaded.
                 */
                if (itemSets[i].getItems().length == 0) {
                    thisAction = ShareProjectAction.UPLOAD;
                }
                /* Exists on the server, but is a file. This can't work. */
                else if (itemSets[i].getItems()[0].getItemType() == ItemType.FILE) {
                    final String messageFormat = Messages.getString("ShareWizard.ErrorDialogTextFormat"); //$NON-NLS-1$
                    final String message = MessageFormat.format(
                        messageFormat,
                        projects[i].getName(),
                        itemSets[i].getItems()[0].getServerItem());

                    MessageDialog.openError(
                        shell,
                        Messages.getString("ShareWizard.ProjectMappedToFileDialogTitle"), //$NON-NLS-1$
                        message);
                    return null;
                } else {
                    thisAction = ShareProjectAction.CONNECT;
                }

                if (action == null) {
                    action = thisAction;
                } else if (action != thisAction) {
                    return null;
                }
            }

            return action;
        }

        /* Mismatch: some projects have working folder mappings, some do not */
        return null;
    }

    @Override
    protected boolean enableFinish(final IWizardPage currentPage) {
        if (ShareWizardSingleProjectConfirmationPage.PAGE_NAME.equals(currentPage.getName())
            || ShareWizardMultipleProjectConfirmationPage.PAGE_NAME.equals(currentPage.getName())) {
            return true;
        }

        return false;
    }

    private boolean buildConfiguration() {
        final TFSTeamProjectCollection connection =
            (TFSTeamProjectCollection) getPageData(TFSTeamProjectCollection.class);
        final Workspace workspace = (Workspace) getPageData(Workspace.class);
        final IProject[] projects = (IProject[]) getPageData(IProject.class);
        final Map<IProject, String> projectWorkingFolderMap =
            (Map<IProject, String>) getPageData(ShareWizardWorkspacePage.PROJECT_WORKING_FOLDER_MAP);
        String[] serverPaths;

        Check.notNull(connection, "connection"); //$NON-NLS-1$
        Check.notNull(workspace, "workspace"); //$NON-NLS-1$
        Check.notNullOrEmpty(projects, "projects"); //$NON-NLS-1$

        final ShareProjectAction action = getAction();

        /*
         * Determine the server paths for these projects. If they have existing
         * working folder mappings, we'll use those. Otherwise, we'll use what
         * was selected in ShareWizardTreePage.
         */
        if (action == ShareProjectAction.CONNECT || action == ShareProjectAction.UPLOAD) {
            serverPaths = new String[projects.length];

            for (int i = 0; i < serverPaths.length; i++) {
                serverPaths[i] = projectWorkingFolderMap.get(projects[i]);
            }
        } else if (action == ShareProjectAction.MAP_AND_UPLOAD) {
            serverPaths = (String[]) getPageData(ShareWizardTreePage.SERVER_PATHS);
        } else {
            /*
             * Do not currently support connecting some projects, uploading
             * others
             */
            MessageDialog.openError(
                getShell(),
                Messages.getString("ShareWizard.InvalidConfigurationDialogTitle"), //$NON-NLS-1$
                Messages.getString("ShareWizard.NonMappedProjectsErrorDialogText")); //$NON-NLS-1$
            return false;
        }

        /* Build the configuration */
        final ShareProjectConfiguration[] configuration = new ShareProjectConfiguration[projects.length];

        for (int i = 0; i < projects.length; i++) {
            configuration[i] = new ShareProjectConfiguration(projects[i], action, serverPaths[i]);
        }

        setPageData(ShareProjectConfiguration.class, configuration);
        return true;
    }

    @Override
    protected boolean doPerformFinish() {
        final TFSTeamProjectCollection connection =
            (TFSTeamProjectCollection) getPageData(TFSTeamProjectCollection.class);
        final Workspace workspace = (Workspace) getPageData(Workspace.class);
        final IProject[] projects = (IProject[]) getPageData(IProject.class);
        final ShareProjectConfiguration[] configuration =
            (ShareProjectConfiguration[]) getPageData(ShareProjectConfiguration.class);

        Check.notNull(connection, "connection"); //$NON-NLS-1$
        Check.notNull(workspace, "workspace"); //$NON-NLS-1$
        Check.notNullOrEmpty(projects, "projects"); //$NON-NLS-1$

        /* Create a repository, or get the existing one. */
        final RepositoryStatusContainer repositoryStatus = new RepositoryStatusContainer();

        final TFSServer server = TFSEclipseClientPlugin.getDefault().getServerManager().getOrCreateServer(
            workspace.getClient().getConnection());
        final TFSRepository repository =
            TFSEclipseClientPlugin.getDefault().getRepositoryManager().getOrCreateRepository(
                workspace,
                repositoryStatus);

        /*
         * Defer synchronize auto-refreshes (based on core events) temporarily.
         * We want to pend adds before we hook the project to the
         * ProjectRepositoryManager. This would otherwise cause a race where we
         * try to sync before the project is "hooked up".
         */
        SynchronizeSubscriber.getInstance().deferAutomaticRefresh();

        final boolean failure;

        try {
            final ShareProjectsCommand shareCommand = new ShareProjectsCommand(repository, configuration);
            failure = (getCommandExecutor().execute(shareCommand).getSeverity() > IStatus.WARNING);
        } finally {
            SynchronizeSubscriber.getInstance().continueAutomaticRefresh();
        }

        if (failure) {
            /*
             * If we created the repository, unhook it to allow for users to set
             * a new connection.
             */
            if (repositoryStatus.getRepositoryStatus().equals(RepositoryStatus.CREATED)) {
                /*
                 * Sanity check: Make sure that there are no other projects
                 * using this workspace. This could happen if the share
                 * partially succeeded. We *should* roll back in this case, and
                 * unhook ourselves as the repository provider (and thus unhook
                 * the projects mapped to the repository.) But this will lead to
                 * pain enough that we should catch this case.
                 */
                if (TFSEclipseClientPlugin.getDefault().getProjectManager().getProjectsForRepository(
                    repository).length > 0) {
                    final String messageFormat =
                        "Couldn't unhook newly created repository {0} because other projects are using it"; //$NON-NLS-1$
                    final String message = MessageFormat.format(messageFormat, repository.getName());
                    logger.error(message);
                } else {
                    repository.close();
                    server.close();

                    TFSEclipseClientPlugin.getDefault().getRepositoryManager().removeRepository(repository);
                    TFSEclipseClientPlugin.getDefault().getServerManager().removeServer(server);
                }
            }

            return false;
        }

        /*
         * Register this workspace with the RepositoryManager, and register
         * these projects with the ItemCache
         */
        finishConnection();
        finishWorkspace((Workspace) getPageData(Workspace.class));

        /*
         * If this is a local workspace, there may be baseline folders ($tf/.tf)
         * which we need to mark as "team private" resources. Resource Change
         * Listener might eventually catch these, but we can do a quick search
         * here.
         *
         * Also scan for pending changes.
         */
        if (workspace.getLocation() == WorkspaceLocation.LOCAL) {
            final List<String> pathsToScan = new ArrayList<String>();
            for (final IProject project : projects) {
                pathsToScan.add(project.getLocation().toOSString());
                try {
                    TeamUtils.markBaselineFoldersTeamPrivate(project);
                } catch (final CoreException e) {
                    logger.warn("Error during share marking baseline folders as team private members", e); //$NON-NLS-1$
                }
            }

            UICommandExecutorFactory.newUICommandExecutor(getShell()).execute(
                new ScanLocalWorkspaceCommand(repository, pathsToScan));
        }

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

                // Ensure the pending changes page is shown in Team Explorer.
                teamExplorerView.navigateToPageID(TeamExplorerPendingChangesPage.ID);
            } catch (final PartInitException e) {
                logger.warn("Could not open Team Explorer View", e); //$NON-NLS-1$
            }
        }

        CodeMarkerDispatch.dispatch(CODEMARKER_SHARE_COMPLETE);

        return true;
    }

    Map<IProject, String> createProjectWorkingFolderMap(final Workspace workspace) {
        final Map<IProject, String> projectWorkingFolderMap = new HashMap<IProject, String>();

        final IProject[] projects = (IProject[]) getPageData(IProject.class);
        Check.notNullOrEmpty(projects, "projects"); //$NON-NLS-1$

        final WorkingFolder[] workingFolders = workspace.getFolders();

        if (workingFolders == null || workingFolders.length == 0) {
            return projectWorkingFolderMap;
        }

        /*
         * For each project, examine each working folder mapping to determine if
         * the working folder mapping is at (or above) the project root
         * location. If so, we do not need to prompt the user to provide a
         * destination because it is implied in the existing working folder
         * mapping.
         */
        for (int i = 0; i < projects.length; i++) {
            final String serverPath = workspace.getMappedServerPath(projects[i].getLocation().toOSString());

            if (serverPath != null) {
                projectWorkingFolderMap.put(projects[i], serverPath);
            }
        }

        return projectWorkingFolderMap;
    }

    /**
     * Handle adaptability to new-style (3.4+) configuration wizard (
     * {@link org.eclipse.team.ui.IConfigurationWizardExtension}).
     */
    @Override
    public Object getAdapter(final Class adapter) {
        /* Use reflection to get the IConfigurationWizardExtension interface */
        try {
            final Class extensionClass = Class.forName("org.eclipse.team.ui.IConfigurationWizardExtension"); //$NON-NLS-1$

            if (adapter.equals(extensionClass)) {
                return new ShareWizardExtension(this);
            }
        } catch (final Exception e) {
            logger.info("Could not obtain IConfigurationWizardExtension class.  Deferring to old-style behavior."); //$NON-NLS-1$
        }

        return null;
    }
}
