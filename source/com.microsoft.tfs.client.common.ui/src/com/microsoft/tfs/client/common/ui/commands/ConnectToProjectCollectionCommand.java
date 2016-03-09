// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.commands;

import java.net.URI;
import java.text.MessageFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;

import com.microsoft.tfs.client.common.commands.TFSCommand;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.config.UIClientConnectionAdvisor;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.httpclient.Credentials;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.LocaleUtil;

/**
 * Connects to the {@link TFSTeamProjectCollection} by the given {@link Profile}
 * .
 *
 * @threadsafety unknown
 */
public class ConnectToProjectCollectionCommand extends TFSCommand implements ConnectCommand {
    private static final Log log = LogFactory.getLog(ConnectToProjectCollectionCommand.class);

    private final URI serverURI;
    private final Credentials credentials;

    private TFSTeamProjectCollection connection;

    public ConnectToProjectCollectionCommand(final URI serverURI, final Credentials credentials) {
        Check.notNull(serverURI, "serverURI"); //$NON-NLS-1$
        Check.notNull(credentials, "credentials"); //$NON-NLS-1$

        this.serverURI = serverURI;
        this.credentials = credentials;

        setCancellable(true);
        addExceptionHandler(new ConnectCommandExceptionHandler());
    }

    @Override
    public String getName() {
        final String messageFormat = Messages.getString("ConnectToProjectCollectionCommand.ConnectToServerFormat"); //$NON-NLS-1$
        final String message = MessageFormat.format(messageFormat, serverURI.toString());

        return message;
    }

    @Override
    public String getErrorDescription() {
        return (Messages.getString("ConnectToProjectCollectionCommand.CommandErrorText")); //$NON-NLS-1$
    }

    @Override
    public String getLoggingDescription() {
        return (Messages.getString("ConnectToProjectCollectionCommand.CommandName", LocaleUtil.ROOT)); //$NON-NLS-1$
    }

    @Override
    protected IStatus doRun(final IProgressMonitor progressMonitor) throws Exception {
        final String message = getName();

        log.info(message);

        progressMonitor.beginTask(message, IProgressMonitor.UNKNOWN);

        connection = new TFSTeamProjectCollection(serverURI, credentials, new UIClientConnectionAdvisor());

        checkForCancellation(progressMonitor);

        try {
            connection.authenticate();
        } catch (final Exception e) {
            if (connection != null) {
                connection.close();
            }
            throw e;
        }

        return null;
    }

    @Override
    public TFSTeamProjectCollection getConnection() {
        return connection;
    }
}
