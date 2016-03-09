// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.config.client;

import com.microsoft.tfs.core.TFSConnection;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.commonstructure.ProjectInfo;
import com.microsoft.tfs.core.clients.sharepoint.WSSClient;
import com.microsoft.tfs.core.config.ConnectionAdvisor;

/**
 * <p>
 * An {@link ClientFactory} is used by a {@link TFSConnection} to create
 * clients. An {@link ClientFactory} is supplied to a {@link TFSConnection} by a
 * {@link ConnectionAdvisor}.
 * </p>
 * <p>
 * {@link TFSConnection} allows only a single thread to use a
 * {@link DefaultClientFactory} at a time.
 * </p>
 * <p>
 * For a default implementation, see {@link DefaultClientFactory}.
 * </p>
 *
 * @see TFSConnection
 * @see ConnectionAdvisor
 * @see DefaultClientFactory
 *
 * @since TEE-SDK-10.1
 * @threadsafety thread-compatible
 */
public interface ClientFactory {
    /**
     * Creates a new client.
     *
     *
     * @param clientType
     *        the client type to create (must not be <code>null</code>)
     * @param connection
     *        the {@link TFSConnection} the client is being created for (must
     *        not be <code>null</code>)
     * @throws UnknownClientException
     *         if the client type is unknown
     * @return a new client instance (never <code>null</code>)
     */
    public Object newClient(Class clientType, TFSConnection connection);

    /**
     * Creates a new Sharepoint client.
     *
     * @param connection
     *        the {@link TFSTeamProjectCollection} the client is being created
     *        for (must not be <code>null</code>)
     * @param projectInfo
     *        the team project to create a sharepoint client for
     * @return a new sharepoint client instance (never <code>null</code>)
     */
    public WSSClient newWSSClient(TFSTeamProjectCollection connection, ProjectInfo projectInfo);
}
