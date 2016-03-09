// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.framework;

import com.microsoft.tfs.core.FrameworkServerDataProvider;
import com.microsoft.tfs.core.PreFrameworkServerDataProvider;
import com.microsoft.tfs.core.ServerCapabilities;
import com.microsoft.tfs.core.TFSConnection;
import com.microsoft.tfs.core.clients.framework.location.ConnectOptions;
import com.microsoft.tfs.core.clients.framework.location.ILocationService;
import com.microsoft.tfs.core.clients.webservices.TeamFoundationIdentity;
import com.microsoft.tfs.util.GUID;

/**
 * Provides information about the server a {@link TFSConnection} is connected
 * to. An abstraction of server identification; implementations exist for
 * pre-TFS 2010 and post-TFS 2010 services which provide the actual data.
 *
 * @see FrameworkServerDataProvider
 * @see PreFrameworkServerDataProvider
 *
 * @threadsafety thread-safe
 * @since TEE-SDK-10.1
 */
public interface ServerDataProvider extends ILocationService {
    public GUID getInstanceID();

    public GUID getCatalogResourceID();

    public TeamFoundationIdentity getAuthorizedIdentity();

    public TeamFoundationIdentity getAuthenticatedIdentity();

    public boolean hasAuthenticated();

    public void ensureAuthenticated();

    public void authenticate();

    /**
     * The capabilities of the TFS server.
     */

    public ServerCapabilities getServerCapabilities();

    /**
     * The function finds the location of the server that has the guid passed.
     * Note that the server in question must be a "child" server of the server
     * this object is providing data for.
     *
     * @param serverGUID
     *        the GUID for the server we are looking up
     * @return the location URI for the server with the provided GUID or
     *         <code>null</code> if this server does not have a child with the
     *         provided GUID
     */
    public String findServerLocation(GUID serverGUID);

    /**
     * Performs all of the steps that are necessary for setting up a connection
     * with a Team Foundation Server. Specify what information should be
     * returned in the connectOptions parameter.
     *
     * Each time this call is made the username for the current user will be
     * returned as well as the client zone that this client is making requests
     * from.
     *
     * @param connectOptions
     *        Specifies what information that should be returned from the
     *        server.
     */
    public void connect(ConnectOptions connectOptions);

    /**
     * Clears any caches that it has if the server has been updated.
     */
    public void reactToPossibleServerUpdate(int serverLastChangeId);
}
