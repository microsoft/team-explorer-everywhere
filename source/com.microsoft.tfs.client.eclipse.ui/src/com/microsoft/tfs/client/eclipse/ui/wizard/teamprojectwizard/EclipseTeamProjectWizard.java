// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.ui.wizard.teamprojectwizard;

import java.net.URI;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.wizard.IWizardPage;

import com.microsoft.tfs.client.common.codemarker.CodeMarker;
import com.microsoft.tfs.client.common.codemarker.CodeMarkerDispatch;
import com.microsoft.tfs.client.common.framework.background.BackgroundTask;
import com.microsoft.tfs.client.common.framework.background.IBackgroundTask;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.server.TFSServer;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.framework.image.ImageHelper;
import com.microsoft.tfs.client.common.ui.wizard.connectwizard.ConnectWizard;
import com.microsoft.tfs.client.common.ui.wizard.teamprojectwizard.ITeamProjectWizard;
import com.microsoft.tfs.client.eclipse.TFSEclipseClientPlugin;
import com.microsoft.tfs.client.eclipse.ui.Messages;
import com.microsoft.tfs.client.eclipse.ui.TFSEclipseClientUIPlugin;
import com.microsoft.tfs.client.eclipse.ui.wizard.connectwizard.EclipseConnectWizard;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.commonstructure.ProjectInfo;
import com.microsoft.tfs.core.clients.versioncontrol.SourceControlCapabilityFlags;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;

public final class EclipseTeamProjectWizard extends EclipseConnectWizard implements ITeamProjectWizard {
    public static final CodeMarker CODEMARKER_NOTIFICATION_WIZARD_FINISH =
        new CodeMarker("com.microsoft.tfs.client.eclipse.ui.wizard.teamprojectwizard.EclipseTeamProjectWizard#finsh"); //$NON-NLS-1$

    private static final Log log = LogFactory.getLog(EclipseTeamProjectWizard.class);

    private final static ImageHelper imageHelper = new ImageHelper(TFSCommonUIClientPlugin.PLUGIN_ID);

    public EclipseTeamProjectWizard() {
        super(
            Messages.getString("EclipseTeamProjectWizard.WizardTitle"), //$NON-NLS-1$
            Messages.getString("EclipseTeamProjectWizard.WizardDescription"), //$NON-NLS-1$
            imageHelper.getImageDescriptor("images/wizard/pageheader.png"), //$NON-NLS-1$
            SourceControlCapabilityFlags.GIT_TFS,
            ConnectWizard.PROJECT_SELECTION);

        addConnectionPages();
        initConnectionPages();

        if (hasPageData(Workspace.class)) {
            removePageData(Workspace.class);
        }
    }

    @Override
    public void setServerURI(final URI serverURI) {
        setPageData(URI.class, serverURI);
    }

    @Override
    public boolean enableNext(final IWizardPage currentPage) {
        if (!enableNextConnectionPage(currentPage)) {
            return false;
        }

        /*
         * Override super's behavior, if the current page is the team project
         * page, we don't want next to occur (hide the workspace page)
         */
        if (getSelectionPageName().equals(currentPage.getName())) {
            return false;
        }

        return true;
    }

    @Override
    public IWizardPage getNextPage(final IWizardPage page) {
        final IWizardPage nextConnectionPage = getNextConnectionPage();

        if (nextConnectionPage != null) {
            return nextConnectionPage;
        }

        /*
         * If we got to this point, then we were started from an already
         * connected state. This means that we only show the team project page.
         */
        return getPage(getSelectionPageName());
    }

    @Override
    protected boolean enableFinish(final IWizardPage currentPage) {
        /*
         * Finish is enabled for the team project page iff we already have a
         * workspace.
         */
        if (getSelectionPageName().equals(currentPage.getName())) {
            return true;
        }

        return false;
    }

    @Override
    protected boolean doPerformFinish() {
        /*
         * Create a dummy server manager connection job for the UI - this will
         * prevent various views from saying "Not Connected" while we're hooking
         * up the various Plugin TFSServer and TFSRepository data in the
         * background.
         */
        final IBackgroundTask backgroundTask =
            new BackgroundTask(Messages.getString("EclipseTeamProjectWizard.InitializingConnectionMessage")); //$NON-NLS-1$

        TFSEclipseClientPlugin.getDefault().getServerManager().backgroundConnectionTaskStarted(backgroundTask);

        try {
            final TFSTeamProjectCollection connection =
                (TFSTeamProjectCollection) getPageData(TFSTeamProjectCollection.class);

            /* See if there's an existing connection to a different server */
            final TFSServer existingServer = TFSEclipseClientPlugin.getDefault().getServerManager().getDefaultServer();

            /* See if there's an existing connection to a different workspace */
            final TFSRepository existingRepository =
                TFSEclipseClientPlugin.getDefault().getRepositoryManager().getDefaultRepository();

            final Workspace[] workspaces = getCurrentWorkspaces(connection);

            /*
             * If the user is connecting to a different server, then we prompt
             * them to close their existing mapped projects for this to
             * continue.
             */
            if ((existingServer != null && !existingServer.connectionsEquivalent(connection))) {
                if (!TFSEclipseClientUIPlugin.getDefault().getConnectionConflictHandler().resolveServerConflict()) {
                    CodeMarkerDispatch.dispatch(CODEMARKER_NOTIFICATION_WIZARD_FINISH);
                    return false;
                }

                /* Ensure that the conflict was successfully resolved. */
                if (TFSEclipseClientUIPlugin.getDefault().getServerManager().getDefaultServer() != null) {
                    TFSEclipseClientUIPlugin.getDefault().getConnectionConflictHandler().notifyServerConflict();
                    CodeMarkerDispatch.dispatch(CODEMARKER_NOTIFICATION_WIZARD_FINISH);
                    return false;
                }
            }

            /*
             * If the user is connecting to a different workspace, prompt to
             * close their existing mapped projects for this to continue.
             */
            else if (workspaces != null && existingRepository != null) {
                boolean containsCurrentWorkspace = false;
                final Workspace currentWorkspace = existingRepository.getWorkspace();
                for (final Workspace ws : workspaces) {
                    if (currentWorkspace.equals(ws)) {
                        containsCurrentWorkspace = true;
                    }
                }
                if (!containsCurrentWorkspace) {
                    if (!TFSEclipseClientUIPlugin.getDefault().getConnectionConflictHandler().resolveRepositoryConflict()
                        || TFSEclipseClientUIPlugin.getDefault().getRepositoryManager().getDefaultRepository() != null) {
                        TFSEclipseClientUIPlugin.getDefault().getConnectionConflictHandler().notifyRepositoryConflict();
                        CodeMarkerDispatch.dispatch(CODEMARKER_NOTIFICATION_WIZARD_FINISH);
                        return false;
                    }
                }
            }

            finishConnection();

            /* get the default workspace */
            final Workspace workspace = getDefaultWorkspace(connection);

            finishWorkspace(workspace);
        } finally {
            TFSEclipseClientPlugin.getDefault().getServerManager().backgroundConnectionTaskFinished(backgroundTask);
        }

        CodeMarkerDispatch.dispatch(CODEMARKER_NOTIFICATION_WIZARD_FINISH);
        return true;
    }

    @Override
    public TFSServer getServer() {
        return (TFSServer) getPageData(TFSServer.class);
    }

    @Override
    public ProjectInfo[] getSelectedProjects() {
        return (ProjectInfo[]) getPageData(ConnectWizard.SELECTED_TEAM_PROJECTS);
    }

}
