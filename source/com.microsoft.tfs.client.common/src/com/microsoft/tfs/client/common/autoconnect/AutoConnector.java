// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.autoconnect;

/**
 * Connects the running product to a Team Foundation Server.
 *
 * @threadsafety thread-compatible
 * @since TEE-SDK-10.1
 */
public interface AutoConnector {
    /**
     * Starts the auto connector, which may start a connection to the last-used
     * server. It is permissible to call {@link #start()} multiple times,
     * however subsequent calls will have no effect.
     *
     * @return true if this {@link AutoConnector} is connecting to a server,
     *         false otherwise
     */
    void start();

    /**
     * Determines whether the auto connector has started.
     *
     * @return true if this {@link AutoConnector} has started, false otherwise.
     */
    boolean isStarted();

    /**
     * Determines whether the auto connector is currently connecting the
     * last-used connection.
     *
     * @return true if this {@link AutoConnector} is connecting, false
     *         otherwise.
     */
    boolean isConnecting();
}
