// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.config;

import java.net.URI;
import java.util.concurrent.atomic.AtomicReference;

import com.microsoft.tfs.core.TFSConfigurationServer;
import com.microsoft.tfs.core.TFSConnection;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.httpclient.Credentials;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.GUID;

/**
 * <p>
 * A holder class for {@link TFSTeamProjectCollection} instance configuration
 * data.
 * </p>
 *
 * @since TEE-SDK-10.1
 * @threadsafety immutable
 */
public class ConnectionInstanceData {
    /**
     * The server URI of a {@link TFSConnection} (never <code>null</code>).
     */
    private final URI serverURI;

    /**
     * The session ID of a {@link TFSTeamProjectCollection} (never
     * <code>null</code>).
     */
    private final GUID sessionId;

    /**
     * An atomic reference to the credentials for this {@link TFSConnection}.
     * Allows us to share the reference between multiple
     * {@link ConnectionInstanceData}s.
     */
    private final AtomicReference<Credentials> credentialsHolder;

    /**
     * Creates a new {@link ConnectionInstanceData}.
     *
     * @param profile
     *        the {@link TFSTeamProjectCollection}'s profile (must not be
     *        <code>null</code> )
     * @param sessionId
     *        the {@link TFSTeamProjectCollection}'s session ID (must not be
     *        <code>null</code>)
     */
    public ConnectionInstanceData(final URI serverURI, final GUID sessionId) {
        this(serverURI, new AtomicReference<Credentials>(null), sessionId);
    }

    /**
     * Creates a new {@link ConnectionInstanceData}.
     *
     * @param profile
     *        the {@link TFSTeamProjectCollection}'s profile (must not be
     *        <code>null</code>)
     * @param credentials
     *        the {@link Credentials} to use when connecting
     * @param sessionId
     *        the {@link TFSTeamProjectCollection}'s session ID (must not be
     *        <code>null</code>)
     */
    public ConnectionInstanceData(
        final URI serverURI,
        final AtomicReference<Credentials> credentialsHolder,
        final GUID sessionId) {
        Check.notNull(serverURI, "serverURI"); //$NON-NLS-1$
        Check.notNull(credentialsHolder, "credentialsHolder"); //$NON-NLS-1$
        Check.notNull(sessionId, "sessionId"); //$NON-NLS-1$

        this.serverURI = serverURI;
        this.credentialsHolder = credentialsHolder;
        this.sessionId = sessionId;
    }

    /**
     * @return this {@link ConnectionInstanceData}'s server URI (never
     *         <code>null</code>)
     */
    public URI getServerURI() {
        return serverURI;
    }

    /**
     * @return a reference to the credentials used by this server, and
     *         potentially other related servers.
     */
    public AtomicReference<Credentials> getCredentialsHolder() {
        return credentialsHolder;
    }

    /**
     * Sets the credentials for this server. Note that the credentials for any
     * related server will be updated. (Ie, the {@link TFSTeamProjectCollection}
     * s created from {@link TFSConfigurationServer} will be updated.)
     *
     * @param credentials
     */
    public void setCredentials(final Credentials credentials) {
        credentialsHolder.set(credentials);
    }

    /**
     * @return the {@link Credentials} for this server
     */
    public Credentials getCredentials() {
        return credentialsHolder.get();
    }

    /**
     * @return this {@link ConnectionInstanceData}'s session ID (never
     *         <code>null</code>)
     */
    public GUID getSessionID() {
        return sessionId;
    }
}
