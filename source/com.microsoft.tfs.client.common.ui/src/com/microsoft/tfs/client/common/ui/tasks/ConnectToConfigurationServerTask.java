// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.tasks;

import java.net.URI;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.ui.commands.ConnectCommand;
import com.microsoft.tfs.client.common.ui.commands.ConnectToConfigurationServerCommand;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.httpclient.Credentials;

/**
 * Connects to a {@link TFSTeamProjectCollection}.
 *
 * @threadsafety unknown
 */
public class ConnectToConfigurationServerTask extends ConnectTask {
    private static final Log log = LogFactory.getLog(ConnectToConfigurationServerTask.class);

    /**
     * Connects to the given server URI.
     *
     * @param shell
     *        a valid {@link Shell}
     * @param URI
     *        the server URI to connect to
     */
    public ConnectToConfigurationServerTask(final Shell shell, final URI serverURI) {
        super(shell, serverURI, null);
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
    public ConnectToConfigurationServerTask(final Shell shell, final URI serverURI, final Credentials credentials) {
        super(shell, serverURI, credentials);
    }

    @Override
    protected ConnectCommand getConnectCommand(final URI serverURI, final Credentials credentials) {
        return new ConnectToConfigurationServerCommand(serverURI, credentials);
    }
}
