// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.util.serverlist;

import java.net.URI;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import com.microsoft.tfs.core.util.ServerURIComparator;
import com.microsoft.tfs.util.Check;

/**
 *
 *
 * @threadsafety unknown
 */
public class ServerList {
    private final Set<ServerListConfigurationEntry> serverSet = new TreeSet<ServerListConfigurationEntry>();

    private final Map<URI, ServerListConfigurationEntry> uriMap =
        new TreeMap<URI, ServerListConfigurationEntry>(ServerURIComparator.INSTANCE);

    public void add(final ServerListConfigurationEntry server) {
        Check.notNull(server, "server"); //$NON-NLS-1$

        final URI uri = server.getURI();

        final ServerListConfigurationEntry existingEntry = uriMap.remove(uri);

        if (existingEntry != null) {
            serverSet.remove(existingEntry);
        }

        serverSet.add(server);
        uriMap.put(uri, server);
    }

    public void addAll(final Collection<ServerListConfigurationEntry> servers) {
        Check.notNull(servers, "servers"); //$NON-NLS-1$

        for (final ServerListConfigurationEntry server : servers) {
            add(server);
        }
    }

    public void remove(final ServerListConfigurationEntry server) {
        Check.notNull(server, "server"); //$NON-NLS-1$

        if (serverSet.remove(server)) {
            uriMap.remove(server.getURI());
        }
    }

    public void removeAll(final Collection<ServerListConfigurationEntry> servers) {
        Check.notNull(servers, "servers"); //$NON-NLS-1$

        for (final ServerListConfigurationEntry server : servers) {
            remove(server);
        }
    }

    public boolean contains(final URI uri) {
        return uriMap.containsKey(uri);
    }

    public boolean contains(final ServerListConfigurationEntry server) {
        return serverSet.contains(server);
    }

    public ServerListConfigurationEntry getServer(final URI uri) {
        return uriMap.get(uri);
    }

    public Set<ServerListConfigurationEntry> getServers() {
        return serverSet;
    }
}
