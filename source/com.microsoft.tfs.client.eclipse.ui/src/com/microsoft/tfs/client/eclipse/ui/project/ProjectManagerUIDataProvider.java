// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.ui.project;

import java.net.URI;
import java.text.MessageFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.connectionconflict.ConnectionConflictHandler;
import com.microsoft.tfs.client.common.credentials.EclipseCredentialsManagerFactory;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.config.UIClientConnectionAdvisor;
import com.microsoft.tfs.client.common.ui.dialogs.connect.CredentialsDialog;
import com.microsoft.tfs.client.common.ui.framework.helper.ShellUtils;
import com.microsoft.tfs.client.common.ui.framework.helper.UIHelpers;
import com.microsoft.tfs.client.common.ui.helpers.EditorHelper;
import com.microsoft.tfs.client.common.ui.helpers.TFSEditorSaveableFilter;
import com.microsoft.tfs.client.common.ui.helpers.TFSEditorSaveableFilter.TFSEditorSaveableType;
import com.microsoft.tfs.client.common.ui.helpers.UIConnectionPersistence;
import com.microsoft.tfs.client.common.ui.prefs.UIPreferenceConstants;
import com.microsoft.tfs.client.common.ui.tasks.vc.DetectLocalChangesTask;
import com.microsoft.tfs.client.eclipse.project.ProjectManagerDataProvider;
import com.microsoft.tfs.client.eclipse.ui.Messages;
import com.microsoft.tfs.client.eclipse.ui.TFSEclipseClientUIPlugin;
import com.microsoft.tfs.client.eclipse.ui.offline.ResourceOfflineSynchronizerFilter;
import com.microsoft.tfs.client.eclipse.ui.offline.ResourceOfflineSynchronizerProvider;
import com.microsoft.tfs.client.eclipse.ui.wizard.connecterror.EclipseConnectErrorWizard;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.versioncontrol.workspacecache.WorkspaceInfo;
import com.microsoft.tfs.core.config.ConnectionAdvisor;
import com.microsoft.tfs.core.config.persistence.DefaultPersistenceStoreProvider;
import com.microsoft.tfs.core.credentials.CachedCredentials;
import com.microsoft.tfs.core.credentials.CredentialsManager;
import com.microsoft.tfs.core.httpclient.Credentials;
import com.microsoft.tfs.util.Check;

public class ProjectManagerUIDataProvider extends ProjectManagerDataProvider {
    private final static Log log = LogFactory.getLog(ProjectManagerUIDataProvider.class);

    public ProjectManagerUIDataProvider() {
        log.debug("Project Manager UI Data Provider started"); //$NON-NLS-1$
    }

    @Override
    public ConnectionAdvisor getConnectionAdvisor() {
        return new UIClientConnectionAdvisor();
    }

    @Override
    public ConnectionConflictHandler getConnectionConflictHandler() {
        return TFSEclipseClientUIPlugin.getDefault().getConnectionConflictHandler();
    }

    @Override
    public TFSTeamProjectCollection promptForConnection(final IProject project) {
        Check.notNull(project, "project"); //$NON-NLS-1$

        final Shell parentShell = ShellUtils.getWorkbenchShell();

        final EclipseConnectErrorWizard connectWizard = new EclipseConnectErrorWizard();

        final String messageFormat =
            Messages.getString("ProjectManagerUiDataProvider.WorkspaceNotLocatedDialogTextFormat"); //$NON-NLS-1$
        final String message = MessageFormat.format(messageFormat, project.getName());
        connectWizard.setErrorMessage(message);

        final WizardDialog connectDialog = new WizardDialog(parentShell, connectWizard);

        if (UIHelpers.openOnUIThread(connectDialog) != IDialogConstants.OK_ID) {
            return null;
        }

        if (connectWizard.hasPageData(TFSTeamProjectCollection.class)) {
            return (TFSTeamProjectCollection) connectWizard.getPageData(TFSTeamProjectCollection.class);
        }

        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Credentials getCredentials(final WorkspaceInfo cachedWorkspace, final Credentials initialCredentials) {
        Check.notNull(cachedWorkspace, "cachedWorkspace"); //$NON-NLS-1$
        Check.notNull(initialCredentials, "initialCredentials"); //$NON-NLS-1$

        final Shell parentShell = ShellUtils.getWorkbenchShell();
        final URI serverURI = cachedWorkspace.getServerURI();

        final CredentialsManager credentialsProvider =
            EclipseCredentialsManagerFactory.getCredentialsManager(DefaultPersistenceStoreProvider.INSTANCE);

        /*
         * Cached credentials may be missing a password, which means we need to
         * prompt.
         */
        final CredentialsDialog credentialsDialog = new CredentialsDialog(parentShell, serverURI);
        credentialsDialog.setCredentials(initialCredentials);
        credentialsDialog.setAllowSavePassword(credentialsProvider.canWrite());

        final int passwordResult = UIHelpers.openOnUIThread(credentialsDialog);

        if (passwordResult == IDialogConstants.OK_ID) {
            final Credentials credentials = credentialsDialog.getCredentials();

            if (credentialsProvider.canWrite() && credentialsDialog.isSavePasswordChecked()) {
                credentialsProvider.setCredentials(new CachedCredentials(serverURI, credentials));
            }

            return credentials;
        } else {
            UIHelpers.runOnUIThread(parentShell, true, new Runnable() {
                @Override
                public void run() {
                    MessageDialog.openInformation(
                        parentShell,
                        Messages.getString("ProjectManagerUiDataProvider.WorkingOfflineDialogTitle"), //$NON-NLS-1$
                        Messages.getString("ProjectManagerUiDataProvider.WorkingOfflineDialogText")); //$NON-NLS-1$
                }
            });

            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Credentials getCredentials(
        final URI serverURI,
        final Credentials failedCredentials,
        final String errorMessage) {
        Check.notNull(serverURI, "serverURI"); //$NON-NLS-1$

        final Shell parentShell = ShellUtils.getWorkbenchShell();

        final CredentialsManager credentialsProvider =
            EclipseCredentialsManagerFactory.getCredentialsManager(DefaultPersistenceStoreProvider.INSTANCE);

        /*
         * Cached credentials may be missing a password, which means we need to
         * prompt.
         */
        final CredentialsDialog credentialsDialog = new CredentialsDialog(parentShell, serverURI);
        credentialsDialog.setCredentials(failedCredentials);
        credentialsDialog.setAllowSavePassword(credentialsProvider.canWrite());
        credentialsDialog.setErrorMessage(errorMessage);

        final int passwordResult = UIHelpers.openOnUIThread(credentialsDialog);

        if (passwordResult == IDialogConstants.OK_ID) {
            final Credentials credentials = credentialsDialog.getCredentials();

            if (credentialsProvider.canWrite() && credentialsDialog.isSavePasswordChecked()) {
                credentialsProvider.setCredentials(new CachedCredentials(serverURI, credentials));
            }

            return credentials;
        } else {
            UIHelpers.runOnUIThread(parentShell, true, new Runnable() {
                @Override
                public void run() {
                    MessageDialog.openInformation(
                        parentShell,
                        Messages.getString("ProjectManagerUiDataProvider.WorkingOfflineDialogTitle"), //$NON-NLS-1$
                        Messages.getString("ProjectManagerUiDataProvider.WorkingOfflineDialogText")); //$NON-NLS-1$
                }
            });

            return null;
        }
    }

    /**
     * Given a profile and a failure that occurred when connecting it, will
     * prompt the user to update the details. Uses extension points to find a
     * contributor who can provide profile details (normally the UI Plug-in.)
     *
     * @param profile
     *        The profile that failed
     * @param failure
     *        The failure that occurred
     * @return The profile to connect with, or null if the user canceled.
     * @param cachedWorkspace
     *        The CachedWorkspace with a null or incomplete profile
     * @return A Profile to connect this CachedWorkspace, or null if none could
     *         be obtained or the user canceled.
     */
    @Override
    public TFSTeamProjectCollection promptForConnection(
        final URI serverURI,
        final Credentials credentials,
        final String errorMessage) {
        Check.notNull(errorMessage, "errorMessage"); //$NON-NLS-1$

        final Shell parentShell = ShellUtils.getWorkbenchShell();

        final EclipseConnectErrorWizard connectWizard = new EclipseConnectErrorWizard();
        connectWizard.setErrorMessage(errorMessage);

        final WizardDialog connectDialog = new WizardDialog(parentShell, connectWizard);

        if (UIHelpers.openOnUIThread(connectDialog) != IDialogConstants.OK_ID) {
            return null;
        }

        if (connectWizard.hasPageData(TFSTeamProjectCollection.class)) {
            return (TFSTeamProjectCollection) connectWizard.getPageData(TFSTeamProjectCollection.class);
        }

        return null;
    }

    /**
     * Should we automatically reconnect projects ("return online") when a
     * {@link TFSRepository} is available.
     *
     * @return <code>true</code> to automatically reconnect projects,
     *         <code>false</code> otherwise
     */
    @Override
    public boolean shouldReconnectProjects() {
        return TFSCommonUIClientPlugin.getDefault().getPreferenceStore().getBoolean(
            UIPreferenceConstants.RECONNECT_PROJECTS_TO_NEW_REPOSITORIES);
    }

    @Override
    public void notifyConnectionEstablished(final TFSTeamProjectCollection connection) {
        UIConnectionPersistence.getInstance().setLastUsedProjectCollection(connection);
    }

    /**
     * Notifies the data provider that the following {@link IProject}s have been
     * automatically reconnected because a {@link TFSRepository} is available
     * for them.
     *
     * @param projects
     *        The {@link IProject}s that were connected (not <code>null</code>)
     */
    @Override
    public void notifyProjectsReconnected(final TFSRepository repository, final IProject[] projects) {
        if (projects == null
            || projects.length == 0
            || TFSCommonUIClientPlugin.getDefault().getPreferenceStore().getBoolean(
                UIPreferenceConstants.DETECT_LOCAL_CHANGES_ON_AUTOMATIC_RECONNECT) == false) {
            return;
        }

        UIHelpers.runOnUIThread(true, new Runnable() {
            @Override
            public void run() {
                // Handles both server and local workspaces
                final DetectLocalChangesTask detectTask = new DetectLocalChangesTask(
                    ShellUtils.getWorkbenchShell(),
                    repository,
                    new ResourceOfflineSynchronizerProvider(projects),
                    new ResourceOfflineSynchronizerFilter());

                detectTask.run();
            }
        });
    }

    /**
     * Prompts users to save dirty work item editors before disconnecting due to
     * projects being closed / disconnected.
     */
    @Override
    public boolean promptForDisconnect() {
        final boolean[] result = new boolean[1];

        UIHelpers.runOnUIThread(false, new Runnable() {
            @Override
            public void run() {
                synchronized (result) {
                    result[0] =
                        EditorHelper.saveAllDirtyEditors(new TFSEditorSaveableFilter(TFSEditorSaveableType.ALL));
                }
            }
        });

        synchronized (result) {
            return result[0];
        }
    }
}
