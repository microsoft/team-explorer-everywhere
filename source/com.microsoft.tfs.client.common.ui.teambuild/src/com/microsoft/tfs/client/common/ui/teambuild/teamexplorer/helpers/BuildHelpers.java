// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teambuild.teamexplorer.helpers;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import com.microsoft.tfs.client.common.server.ServerManager;
import com.microsoft.tfs.client.common.server.TFSServer;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.browser.BrowserFacade.LaunchMode;
import com.microsoft.tfs.client.common.ui.buildmanager.BuildManager;
import com.microsoft.tfs.client.common.ui.framework.command.UICommandExecutorFactory;
import com.microsoft.tfs.client.common.ui.tasks.ViewBuildReportTask;
import com.microsoft.tfs.client.common.ui.teambuild.Messages;
import com.microsoft.tfs.client.common.ui.teambuild.commands.AddRemoveBuildQualitiesCommand;
import com.microsoft.tfs.client.common.ui.teambuild.commands.GetBuildDefinitionCommand;
import com.microsoft.tfs.client.common.ui.teambuild.dialogs.BuildDefinitionDialog;
import com.microsoft.tfs.client.common.ui.teambuild.dialogs.ManageBuildQualitiesDialog;
import com.microsoft.tfs.client.common.ui.teambuild.dialogs.TfsBuildDefinitionDialog;
import com.microsoft.tfs.client.common.ui.teambuild.editors.BuildExplorer;
import com.microsoft.tfs.client.common.ui.teambuild.editors.BuildExplorerEditorInput;
import com.microsoft.tfs.client.common.ui.teambuild.git.EGitHelpers;
import com.microsoft.tfs.client.common.ui.teamexplorer.TeamExplorerContext;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.build.BuildSourceProviders;
import com.microsoft.tfs.core.clients.build.IBuildDefinition;
import com.microsoft.tfs.core.clients.build.IBuildDetail;
import com.microsoft.tfs.core.clients.build.IBuildServer;
import com.microsoft.tfs.core.clients.build.exceptions.BuildException;
import com.microsoft.tfs.core.clients.commonstructure.ProjectInfo;
import com.microsoft.tfs.core.clients.framework.location.ILocationService;
import com.microsoft.tfs.core.clients.framework.location.ServiceDefinition;
import com.microsoft.tfs.core.clients.versioncontrol.SourceControlCapabilityFlags;
import com.microsoft.tfs.util.GUID;

public class BuildHelpers {
    private static final Log log = LogFactory.getLog(BuildHelpers.class);

    public static final String UNC_LOCATION_PREFIX = "\\\\"; //$NON-NLS-1$

    public static final String DROP_TO_FILE_CONTAINER_LOCATION = "#/"; //$NON-NLS-1$

    private final static GUID TIMELINES_ID = new GUID("8baac422-4c6e-4de5-8532-db96d92acffa"); //$NON-NLS-1$

    public static BuildManager getBuildManager() {
        return TFSCommonUIClientPlugin.getDefault().getBuildManager();
    }

    public static boolean newBuildDefinition(final Shell shell, final TeamExplorerContext context) {
        final IBuildServer buildServer = context.getBuildServer();
        final String projectName = context.getCurrentProjectInfo().getName();

        final IBuildDefinition buildDefinition = buildServer.createBuildDefinition(projectName);
        final BuildDefinitionDialog dialog;

        if (isGitProject(context)) {
            buildDefinition.setDefaultSourceProvider(BuildSourceProviders.createGitSourceProvider());
            dialog = (BuildDefinitionDialog) EGitHelpers.getGitBuildDefinitionDialog(shell, buildDefinition);

            if (dialog == null) {
                final String errorMessage = Messages.getString("BuildHelpers.EGitRequired"); //$NON-NLS-1$
                final String title = Messages.getString("BuildHelpers.FailedCreateDefinitionTitle"); //$NON-NLS-1$

                log.error("Cannot edit the build definition. EGit plugin is requered for this action."); //$NON-NLS-1$
                MessageDialog.openError(shell, title, errorMessage);

                return false;
            }
        } else {
            buildDefinition.setDefaultSourceProvider(BuildSourceProviders.createTfVcSourceProvider());
            dialog = new TfsBuildDefinitionDialog(shell, buildDefinition);
        }

        try {
            if (dialog.open() == IDialogConstants.OK_ID) {
                dialog.commitChangesIfNeeded();
                return true;
            }
        } catch (final BuildException e) {
            final String title = Messages.getString("BuildHelpers.CannotCreateDefinitionTitle"); //$NON-NLS-1$
            log.warn(title, e);
            MessageDialog.openWarning(shell, title, e.getMessage());
        } catch (final Exception e) {
            final String title = Messages.getString("BuildHelpers.FailedCreateDefinitionTitle"); //$NON-NLS-1$
            log.error(title, e);
            MessageDialog.openError(shell, title, e.getLocalizedMessage());
        }

        return false;
    }

    public static void manageBuildQualities(final Shell shell, final TeamExplorerContext context) {
        if (BuildExplorer.getInstance() != null
            && !BuildExplorer.getInstance().isDisposed()
            && BuildExplorer.getInstance().getBuildEditorPage() != null) {
            // We have a build explorer visible - check to make sure we are not
            // editing quality
            if (BuildExplorer.getInstance().getBuildEditorPage().getBuildsTableControl().getViewer().isCellEditorActive()) {
                BuildExplorer.getInstance().getBuildEditorPage().getBuildsTableControl().getViewer().cancelEditing();
            }
        }

        final IBuildServer buildServer = context.getBuildServer();
        final String projectName = context.getCurrentProjectInfo().getName();

        final ManageBuildQualitiesDialog dialog = new ManageBuildQualitiesDialog(shell, buildServer, projectName);

        if (dialog.open() == IDialogConstants.OK_ID) {
            final AddRemoveBuildQualitiesCommand command = new AddRemoveBuildQualitiesCommand(
                buildServer,
                projectName,
                dialog.getQualitiesToAdd(),
                dialog.getQualitiesToRemove());

            final IStatus status = UICommandExecutorFactory.newUICommandExecutor(shell).execute(command);
            if (status.getSeverity() == IStatus.OK) {
                // Anything need to be done here?
            }
        }

        // We should redraw and build qualities at this point because the dialog
        // may have loaded new ones from the server into the cache.
        if (BuildExplorer.getInstance() != null
            && !BuildExplorer.getInstance().isDisposed()
            && BuildExplorer.getInstance().getBuildEditorPage() != null) {
            BuildExplorer.getInstance().getBuildEditorPage().reloadBuildQualities();
        }
    }

    public static BuildExplorer openBuildExplorer(
        final TFSTeamProjectCollection collection,
        final IBuildDefinition buildDefinition) {
        try {
            final ServerManager manager = TFSCommonUIClientPlugin.getDefault().getProductPlugin().getServerManager();
            final TFSServer targetServer = manager.getServer(collection);
            final BuildExplorerEditorInput editorInput = new BuildExplorerEditorInput(targetServer, buildDefinition);

            final IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
            final IEditorPart editorPart = page.openEditor(editorInput, BuildExplorer.ID);

            if (editorPart instanceof BuildExplorer) {
                return (BuildExplorer) editorPart;
            }
        } catch (final PartInitException e) {
            throw new RuntimeException(e);
        }

        return null;
    }

    public static void viewTodaysBuildsForDefinition(final IBuildDefinition buildDefinition) {
        final TFSTeamProjectCollection collection = buildDefinition.getBuildServer().getConnection();
        final BuildExplorer buildExplorer = BuildHelpers.openBuildExplorer(collection, buildDefinition);

        if (buildExplorer != null) {
            buildExplorer.showTodaysBuildsForDefinitionView(buildDefinition);
        }
    }

    public static void viewBuildsForDefinition(final IBuildDefinition buildDefinition) {
        final TFSTeamProjectCollection collection = buildDefinition.getBuildServer().getConnection();
        final BuildExplorer buildExplorer = BuildHelpers.openBuildExplorer(collection, buildDefinition);

        if (buildExplorer != null) {
            buildExplorer.activateBuildPage();
            buildExplorer.setBuildDefinition(buildDefinition);
        }
    }

    public static void viewControllerQueue(final IBuildServer buildServer, final IBuildDefinition buildDefinition) {
        final TFSTeamProjectCollection collection = buildServer.getConnection();
        final BuildExplorer buildExplorer = BuildHelpers.openBuildExplorer(collection, buildDefinition);

        if (buildExplorer != null) {
            buildExplorer.showControllerQueueView(buildDefinition.getBuildControllerURI());
        }
    }

    public static void viewBuildReport(final Shell shell, final IBuildDetail buildDetail) {
        viewBuildReport(shell, buildDetail, LaunchMode.USER_PREFERENCE);
    }

    public static void viewBuildReport(final Shell shell, final IBuildDetail buildDetail, final LaunchMode launchMode) {
        new ViewBuildReportTask(
            shell,
            buildDetail.getBuildServer(),
            buildDetail.getURI(),
            buildDetail.getBuildNumber(),
            launchMode).run();
    }

    public static IBuildDefinition getUpToDateBuildDefinition(
        final Shell parentShell,
        final IBuildDefinition buildDefinition) {
        final GetBuildDefinitionCommand command = new GetBuildDefinitionCommand(buildDefinition);
        UICommandExecutorFactory.newBusyIndicatorCommandExecutor(parentShell).execute(command);
        return command.getBuildDefinition();
    }

    public static boolean isGitProject(final TeamExplorerContext context) {
        final ProjectInfo project = context.getCurrentProjectInfo();
        return project.getSourceControlCapabilityFlags().contains(SourceControlCapabilityFlags.GIT);
    }

    public static boolean isTfsProject(final TeamExplorerContext context) {
        final ProjectInfo project = context.getCurrentProjectInfo();
        return project.getSourceControlCapabilityFlags().contains(SourceControlCapabilityFlags.TFS);
    }

    public static boolean isBuildVNextSupported(final TeamExplorerContext context) {
        final IBuildServer buildServer = context.getBuildServer();

        if (buildServer == null || !buildServer.getBuildServerVersion().isV4OrGreater()) {
            return false;
        }

        final ILocationService locationService =
            (ILocationService) context.getServer().getConnection().getClient(ILocationService.class);
        final ServiceDefinition[] definitions = locationService.findServiceDefinitionsByToolType("Framework"); //$NON-NLS-1$
        if (definitions != null) {
            for (int i = 0; i < definitions.length; i++) {
                if (definitions[i].getServiceType().equalsIgnoreCase("build") && //$NON-NLS-1$
                    definitions[i].getIdentifier().equals(TIMELINES_ID)) {
                    return true;
                }
            }
        }

        return false;
    }

}
