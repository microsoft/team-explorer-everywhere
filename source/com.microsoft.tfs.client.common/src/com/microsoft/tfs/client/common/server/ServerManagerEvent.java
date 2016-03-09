// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.server;

import java.util.EventObject;

public class ServerManagerEvent extends EventObject {
    private static final long serialVersionUID = 1388457389900872713L;

    private final TFSServer server;

    public ServerManagerEvent(final ServerManager source, final TFSServer server) {
        super(source);

        this.server = server;
    }

    public ServerManager getServerManager() {
        return (ServerManager) getSource();
    }

    public TFSServer getServer() {
        return server;
    }
}