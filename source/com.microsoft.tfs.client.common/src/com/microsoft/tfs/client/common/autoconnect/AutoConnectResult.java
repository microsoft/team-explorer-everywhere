// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.autoconnect;

import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.server.TFSServer;
import com.microsoft.tfs.util.Check;

/**
 *
 *
 * @threadsafety unknown
 */
public class AutoConnectResult {
    private final TFSServer server;
    private final TFSRepository repository;

    public AutoConnectResult(final TFSServer server, final TFSRepository repository) {
        Check.notNull(server, "server"); //$NON-NLS-1$
        Check.notNull(repository, "repository"); //$NON-NLS-1$

        this.server = server;
        this.repository = repository;
    }

    public TFSServer getServer() {
        return server;
    }

    public TFSRepository getRepository() {
        return repository;
    }
}
