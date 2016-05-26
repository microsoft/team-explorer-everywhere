// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.tasks;

import java.net.URI;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.credentials.EclipseCredentialsManagerFactory;
import com.microsoft.tfs.client.common.framework.command.ICommandExecutor;
import com.microsoft.tfs.client.common.framework.command.ThreadedCancellableCommand;
import com.microsoft.tfs.client.common.framework.status.TeamExplorerStatus;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.commands.ConnectCommand;
import com.microsoft.tfs.client.common.ui.dialogs.connect.CredentialsDialog;
import com.microsoft.tfs.client.common.ui.framework.command.UICommandExecutorFactory;
import com.microsoft.tfs.client.common.ui.framework.command.UICommandFinishedCallbackFactory;
import com.microsoft.tfs.client.common.ui.framework.helper.UIHelpers;
import com.microsoft.tfs.core.TFSConnection;
import com.microsoft.tfs.core.config.persistence.DefaultPersistenceStoreProvider;
import com.microsoft.tfs.core.credentials.CachedCredentials;
import com.microsoft.tfs.core.credentials.CredentialsManagerFactory;
import com.microsoft.tfs.core.exceptions.TFSUnauthorizedException;
import com.microsoft.tfs.core.httpclient.Credentials;
import com.microsoft.tfs.core.httpclient.DefaultNTCredentials;
import com.microsoft.tfs.core.util.CredentialsUtils;
import com.microsoft.tfs.core.ws.runtime.exceptions.TransportRequestHandlerCanceledException;
import com.microsoft.tfs.util.Check;

/**
 * Abstract base class that connects to a server.
 *
 * @threadsafety unknown
 */
public abstract class ConnectTask extends BaseTask {
    private static final Log log = LogFactory.getLog(ConnectToConfigurationServerTask.class);

    private final URI serverURI;
    private Credentials credentials;

    private TFSConnection connection;

    private boolean showErrorDialog = true;

    /**
     * Connects to the given server URI.
     *
     * @param shell
     *        a valid {@link Shell}
     * @param URI
     *        the server URI to connect to
     */
    public ConnectTask(final Shell shell, final URI serverURI) {
        this(shell, serverURI, null);
    }

    /**
     * Connects to the given server URI.
     *
     * @param shell
     *        a valid {@link Shell}
     * @param URI
     *        the server URI to connect to
     * @param credentials
     *        the credentials to connect with (or <code>null</code>)
     */
    public ConnectTask(final Shell shell, final URI serverURI, final Credentials credentials) {
        super(shell);

        Check.notNull(serverURI, "serverURI"); //$NON-NLS-1$

        this.serverURI = serverURI;
        this.credentials = credentials;

        setCommandExecutor(getNoErrorDialogCommandExecutor(shell));
    }

    private static ICommandExecutor getNoErrorDialogCommandExecutor(final Shell shell) {
        final ICommandExecutor noErrorDialogCommandExecutor = UICommandExecutorFactory.newUICommandExecutor(shell);
        noErrorDialogCommandExecutor.setCommandFinishedCallback(
            UICommandFinishedCallbackFactory.getDefaultNoErrorDialogCallback());

        return noErrorDialogCommandExecutor;
    }

    public void setShowErrorDialog(final boolean showErrorDialog) {
        this.showErrorDialog = showErrorDialog;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IStatus run() {
        /* Try to get some credentials */
        if (credentials == null) {
            final CachedCredentials cachedCredentials = EclipseCredentialsManagerFactory.getCredentialsManager(
                DefaultPersistenceStoreProvider.INSTANCE).getCredentials(serverURI);

            // try to use DefaultNTCredentials when no credentials acquired
            credentials = cachedCredentials != null ? cachedCredentials.toCredentials() : new DefaultNTCredentials();
        }

        /*
         * We may have stored credentials that are not complete - if so, prompt.
         */
        if (credentials == null || CredentialsUtils.needsPassword(credentials)) {
            final CredentialsDialog credentialsDialog = new CredentialsDialog(getShell(), serverURI);
            credentialsDialog.setCredentials(credentials);
            credentialsDialog.setAllowSavePassword(
                CredentialsManagerFactory.getCredentialsManager(DefaultPersistenceStoreProvider.INSTANCE).canWrite());

            if (UIHelpers.openOnUIThread(credentialsDialog) != IDialogConstants.OK_ID) {
                return Status.CANCEL_STATUS;
            }

            credentials = credentialsDialog.getCredentials();
        }

        IStatus status = Status.CANCEL_STATUS;

        while (connection == null) {
            final ConnectCommand connectCommand = getConnectCommand(serverURI, credentials);

            status = getCommandExecutor().execute(new ThreadedCancellableCommand(connectCommand));

            connectCommandFinished(connectCommand);

            if (status.isOK()) {
                connection = connectCommand.getConnection();
                break;
            } else if (status.getSeverity() != IStatus.CANCEL) {
                /* See if we can get an Exception out of the error. */
                final Throwable exception = (status instanceof TeamExplorerStatus)
                    ? ((TeamExplorerStatus) status).getTeamExplorerException() : null;

                /* On unauthorized exceptions, prompt for the password again */
                if (exception != null && (exception instanceof TFSUnauthorizedException)) {
                    final CredentialsDialog credentialsDialog = new CredentialsDialog(getShell(), serverURI);
                    credentialsDialog.setErrorMessage(exception.getLocalizedMessage());
                    credentialsDialog.setCredentials(credentials);
                    credentialsDialog.setAllowSavePassword(
                        CredentialsManagerFactory.getCredentialsManager(
                            DefaultPersistenceStoreProvider.INSTANCE).canWrite());

                    if (UIHelpers.openOnUIThread(credentialsDialog) == IDialogConstants.OK_ID) {
                        credentials = credentialsDialog.getCredentials();
                        continue;
                    }
                } else if (exception != null && exception instanceof TransportRequestHandlerCanceledException) {
                    // User canceled; ignore exception
                } else if (showErrorDialog) {
                    if (exception != null) {
                        log.warn("Unexpected connection exception", exception); //$NON-NLS-1$
                    }

                    final IStatus errorStatus = status;

                    UIHelpers.runOnUIThread(false, new Runnable() {
                        @Override
                        public void run() {
                            ErrorDialog.openError(
                                getShell(),
                                Messages.getString("TeamProjectSelectControl.ConnectionFailedDialogTitle"), //$NON-NLS-1$
                                null,
                                errorStatus);
                        }
                    });
                }
            }

            break;
        }

        return status;
    }

    protected abstract ConnectCommand getConnectCommand(final URI serverURI, final Credentials credentials);

    protected void connectCommandFinished(final ConnectCommand connectCommand) {
    }

    public TFSConnection getConnection() {
        return connection;
    }
}
