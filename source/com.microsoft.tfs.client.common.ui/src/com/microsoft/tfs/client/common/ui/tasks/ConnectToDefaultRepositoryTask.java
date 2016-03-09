// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.tasks;

import java.net.URI;

import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.server.TFSServer;
import com.microsoft.tfs.client.common.ui.commands.ConnectCommand;
import com.microsoft.tfs.client.common.ui.commands.ConnectToDefaultRepositoryCommand;
import com.microsoft.tfs.core.httpclient.Credentials;

/**
 * Connects to the "default" (last-used) repository.
 *
 * @threadsafety unknown
 */
public class ConnectToDefaultRepositoryTask extends ConnectTask {
    private TFSServer server;
    private TFSRepository repository;

    public ConnectToDefaultRepositoryTask(final Shell shell, final URI serverURI) {
        this(shell, serverURI, null);
    }

    public ConnectToDefaultRepositoryTask(final Shell shell, final URI serverURI, final Credentials credentials) {
        super(shell, serverURI, credentials);
    }

    @Override
    protected ConnectCommand getConnectCommand(final URI serverURI, final Credentials credentials) {
        return new ConnectToDefaultRepositoryCommand(serverURI, credentials);
    }

    @Override
    protected void connectCommandFinished(final ConnectCommand connectCommand) {
        server = ((ConnectToDefaultRepositoryCommand) connectCommand).getServer();
        repository = ((ConnectToDefaultRepositoryCommand) connectCommand).getRepository();
    }

    public TFSServer getServer() {
        return server;
    }

    public TFSRepository getRepository() {
        return repository;
    }
}
