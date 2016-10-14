// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.autoconnect;

import java.net.URI;
import java.text.MessageFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.autoconnect.AutoConnector;
import com.microsoft.tfs.client.common.framework.status.TeamExplorerStatus;
import com.microsoft.tfs.client.common.license.LicenseManager;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.server.TFSServer;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.commands.ConnectToDefaultRepositoryCommand;
import com.microsoft.tfs.client.common.ui.framework.helper.ShellUtils;
import com.microsoft.tfs.client.common.ui.framework.helper.UIHelpers;
import com.microsoft.tfs.client.common.ui.helpers.UIConnectionPersistence;
import com.microsoft.tfs.client.common.ui.prefs.UIPreferenceConstants;
import com.microsoft.tfs.client.common.ui.protocolhandler.ProtocolHandler;
import com.microsoft.tfs.client.common.ui.tasks.ConnectToDefaultRepositoryTask;
import com.microsoft.tfs.client.common.ui.teamexplorer.helpers.ConnectHelpers;
import com.microsoft.tfs.core.clients.commonstructure.ProjectInfo;
import com.microsoft.tfs.core.exceptions.NotSupportedException;
import com.microsoft.tfs.core.exceptions.TEPreviewExpiredException;
import com.microsoft.tfs.core.util.URIUtils;

/**
 * Base class for Eclipse Plugin and Explorer based auto-connect mechanisms.
 *
 * @threadsafety unknown
 */
public abstract class UIAutoConnector implements AutoConnector {
    private static final Log log = LogFactory.getLog(UIAutoConnector.class);

    private final Object lock = new Object();

    private boolean started = false;
    private boolean connecting = false;

    @Override
    public void start() {
        synchronized (lock) {
            if (started) {
                return;
            }
        }
        /*
         * Schedule this in a job so that it will be started when the workbench
         * is started. (If we're restoring a view, the workbench is not fully
         * started when this method is called. This will prevent deadlocking.)
         */
        final Job connectorJob = new Job(Messages.getString("UIAutoConnector.ConnectingToServer")) //$NON-NLS-1$
        {
            @Override
            protected IStatus run(final IProgressMonitor progressMonitor) {
                startInternal();
                return Status.OK_STATUS;
            }
        };
        connectorJob.setSystem(true);
        connectorJob.schedule();
    }

    private void startInternal() {
        final URI serverURI;
        final boolean startConnection;

        boolean hasProtocolHandlerRequest = ProtocolHandler.getInstance().hasProtocolHandlerRequest();

        synchronized (lock) {
            if (started) {
                return;
            }

            started = true;

            if (hasProtocolHandlerRequest) {
                if (ProtocolHandler.getInstance().hasProtocolHandlerCollectionUrl()) {
                    serverURI = URIUtils.newURI(ProtocolHandler.getInstance().getProtocolHandlerCollectionUrl());
                } else {
                    serverURI = URIUtils.newURI(ProtocolHandler.getInstance().getProtocolHandlerServerUrl());
                }
                log.info("Auto connecting to the server requested by protocol handler: " + serverURI); //$NON-NLS-1$
            } else {
                /* Do not connect if we're already connected to a server */
                if (TFSCommonUIClientPlugin.getDefault().getProductPlugin().getServerManager().getDefaultServer() != null) {
                    return;
                }

                /* Do not autoconnect if the preference is unset */
                if (!TFSCommonUIClientPlugin.getDefault().getPreferenceStore().getBoolean(
                    UIPreferenceConstants.RECONNECT_AT_STARTUP)) {
                    return;
                }

                serverURI = UIConnectionPersistence.getInstance().getLastUsedServerURI();
                if (serverURI != null) {
                    log.info("Auto connecting to the previously used server: " + serverURI); //$NON-NLS-1$
                }
            }
            LicenseManager.getInstance().getProductID();

            /*
             * Auto-connect if the user requested it and the profile is complete
             * (or requires only a password) and the license is not expired or
             * about to expire.
             */
            if (shouldAutoConnect() && serverURI != null && LicenseManager.getInstance().isEULAAccepted()) {
                startConnection = true;
                connecting = true;
            } else {
                if (!shouldAutoConnect()) {
                    log.info("Auto connection is not requested."); //$NON-NLS-1$
                } else if (serverURI == null) {
                    log.info("No previously connected server detected."); //$NON-NLS-1$
                } else {
                    log.info("EULA is not accepted yet."); //$NON-NLS-1$
                }
                startConnection = false;
            }
        }

        try

        {
            if (startConnection) {
                final Shell shell = ShellUtils.getBestParent(ShellUtils.getWorkbenchShell());

                final ConnectToDefaultRepositoryTask connectTask = new ConnectToDefaultRepositoryTask(shell, serverURI);
                connectTask.setShowErrorDialog(false);

                final IStatus status = connectTask.run();

                if (status.isOK()) {
                    final TFSServer server = connectTask.getServer();
                    final TFSRepository repository = connectTask.getRepository();

                    TFSCommonUIClientPlugin.getDefault().getProductPlugin().getRepositoryManager().setDefaultRepository(
                        repository);

                    final ProjectInfo currentTeamProject;
                    if (hasProtocolHandlerRequest) {
                        currentTeamProject = server.getProjectCache().getTeamProject(
                            ProtocolHandler.getInstance().getProtocolHandlerProject());
                        server.getProjectCache().setActiveTeamProjects(new ProjectInfo[] {
                            currentTeamProject
                        });
                        server.getProjectCache().setCurrentTeamProject(currentTeamProject);
                    } else {
                        currentTeamProject = server.getProjectCache().getCurrentTeamProject();
                    }

                    if (currentTeamProject != null) {
                        ConnectHelpers.showHideViews(currentTeamProject.getSourceControlCapabilityFlags());
                    } else {
                        ConnectHelpers.showHideViews(null);
                    }
                } else if (status.getSeverity() == IStatus.CANCEL) {
                    return;
                } else {
                    UIConnectionPersistence.getInstance().clearLastUsedServerURI();

                    final IStatus displayStatus = status;

                    UIHelpers.runOnUIThread(true, new Runnable() {
                        @Override
                        public void run() {
                            /*
                             * For preview timeout exceptions, don't display the
                             * reconnect instructions. Those would be
                             * inappropriate.
                             */
                            final String message;

                            if (displayStatus instanceof TeamExplorerStatus
                                && ((TeamExplorerStatus) displayStatus).getTeamExplorerException() != null
                                && (((TeamExplorerStatus) displayStatus).getTeamExplorerException() instanceof TEPreviewExpiredException
                                    || ((TeamExplorerStatus) displayStatus).getTeamExplorerException() instanceof NotSupportedException)) {
                                message = displayStatus.getMessage();
                            } else {
                                message = MessageFormat.format(
                                    Messages.getString("UIAutoConnector.ReconnectFailedStatusFormat"), //$NON-NLS-1$
                                    displayStatus.getMessage(),
                                    Messages.getString("UIAutoConnector.ReconnectFailedMessage")); //$NON-NLS-1$
                            }

                            MessageDialog.openError(
                                shell,
                                Messages.getString("UIAutoConnector.ReconnectFailedTitle"), //$NON-NLS-1$
                                message);
                        }
                    });
                }
            }
        } finally {
            synchronized (lock) {
                connecting = false;
            }
        }

        return;
    }

    @Override
    public boolean isStarted() {
        synchronized (lock) {
            return started;
        }
    }

    @Override
    public boolean isConnecting() {
        synchronized (lock) {
            return connecting;
        }
    }

    /**
     * Determines whether the product should attempt to auto connect when the
     * base product plugin is started.
     *
     * @return true if the product should auto connect to the last-used server,
     *         false otherwise
     */
    protected abstract boolean shouldAutoConnect();

    /**
     * Gets the command executor for this product. (Must not be
     * <code>null</code>.)
     *
     * @return The command executor to execute the auto connect command with.
     */
    protected abstract IStatus executeConnectCommand(
        final Shell shell,
        final ConnectToDefaultRepositoryCommand command);
}
