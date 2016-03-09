// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.util.serverlist;

/**
 * A simple interface to a list of known servers.
 *
 * @threadsafety unknown
 */
public interface ServerListManager {
    /**
     * Provides the list of known servers.
     *
     * @return The list of servers (never <code>null</code>)
     */
    ServerList getServerList();

    /**
     * Sets the list of known server URIs.
     *
     * @param serverList
     *        The server list.
     */
    boolean setServerList(ServerList serverList);
}
