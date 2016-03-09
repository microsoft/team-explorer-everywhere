// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.server;

import java.util.EventListener;

public interface ServerManagerListener extends EventListener {
    /**
     * Clients will be notified when the given server is added to the server
     * manager and is available for use by the clients. If this server is now
     * the default, clients will also be notified with an
     * {@link #onDefaultServerChanged(ServerManagerEvent)} event immediately
     * following this event.
     *
     * @param event
     *        A {@link ServerManagerEvent} describing the new server (never
     *        <code>null</code>).
     */
    public void onServerAdded(ServerManagerEvent event);

    /**
     * Clients will be notified when the given server is removed from the server
     * manager and is no longer available for use by the clients. If the removed
     * server was the default, clients will also be notified with an
     * {@link #onDefaultServerChanged(ServerManagerEvent)} event immediately
     * following this event.
     *
     * @param event
     *        A {@link ServerManagerEvent} describing the disconnected server
     *        (never <code>null</code>).
     */
    public void onServerRemoved(ServerManagerEvent event);

    /**
     * Clients will be notified when the "default" server is changed. The server
     * referenced in the event may be <code>null</code> if there is now no
     * default server.
     *
     * @param event
     *        A {@link ServerManagerEvent} describing the new default server
     *        (never <code>null</code>).
     */
    public void onDefaultServerChanged(ServerManagerEvent event);
}