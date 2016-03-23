// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.credentials;

import java.net.URI;

/**
 * Manages network authentication credentials. Implementations may use to secure
 * system storage (Mac OS Keychain, Windows Credential Manager) or use a a
 * non-secure storage mechanism (plain text in the filesystem). Call
 * {@link #isSecure()} to test whether the implementation offers secure storage.
 * <p>
 * Implementations are not required to support writing (saving) credentials.
 * <p>
 * Implementations may alter the {@link URI} before it is saved by
 * {@link #setCredentials(CachedCredentials)} and when it is matched by
 * {@link #getCredentials(URI)}. A common alteration is to remove the path and
 * query parts from the URI so only the scheme, host, and port are saved and
 * matched. This lets a user authenticate to all resources on one server (TFS
 * configuration server, TFS project collections, SharePoint sites, etc.) using
 * a single saved credentials item.
 *
 * @threadsafety thread-safe
 */
public interface CredentialsManager {
    /**
     * Gets the localized name of the storage mechanism this provider uses. The
     * string can be in any format but it should be unique among implementations
     * and cannot be empty.
     *
     * @return the human-readable name of the storage mechanism used (never
     *         <code>null</code>)
     */
    String getUIMechanismName();

    /**
     * Tests whether this credentials provider is writable. Some credentials
     * providers provide read-only views of the credentials data.
     *
     * @return <code>true</code> if the credentials provider is writable,
     *         <code>false</code> otherwise
     */
    boolean canWrite();

    /**
     * Queries whether the credentials are stored in a secure mechanism as
     * determined by the operating system, for example, Apple Keychain, GNOME
     * Keyring, Windows CredMan, Eclipse's credential store, or encrypted using
     * a system mechanism such as Windows DPAPI.
     *
     * @return <code>true</code> if these credentials are stored securely,
     *         <code>false</code> otherwise
     */
    boolean isSecure();

    /**
     * Returns all the credentials that are currently configured for any server.
     *
     * @return All credentials that are configured (may be <code>null</code>)
     */
    CachedCredentials[] getCredentials();

    /**
     * Provides the credentials for the given {@link URI}. The given {@link URI}
     * may be a TFS server, an HTTP proxy or a TFS proxy.
     * <p>
     * This method may return {@link CachedCredentials} that match only some
     * parts of the given {@link URI} (instead of all parts).
     *
     * @param serverURI
     *        The URI to connect to (never <code>null</code>)
     * @return The credentials to connect with (never <code>null</code>)
     */
    CachedCredentials getCredentials(URI serverURI);

    /**
     * Sets the credentials for the given {@link URI}. The given {@link URI} may
     * be a TFS server, an HTTP proxy or a TFS proxy.
     * <p>
     * Users should ensure that this credentials provider is writable by calling
     * the {@link #canWrite()} method for calling this one.
     * <p>
     * See class documentation for {@link CredentialsManager} for information on
     * how this {@link URI} may be altered before being saved.
     *
     * @param cachedCredentials
     *        The credentials to connect with (never <code>null</code>)
     * @return <code>true</code> if the credentials were successfully saved,
     *         <code>false</code> otherwise
     * @throws RuntimeException
     *         if the credentials store is not writable
     */
    boolean setCredentials(CachedCredentials cachedCredentials);

    /**
     * Removes the credentials for the given URI. Note that the given username
     * and password need not match for a cached credentials object to be
     * removed, only the server URI need match.
     * <p>
     * Users should ensure that this credentials provider is writable by calling
     * the {@link #canWrite()} method for calling this one.
     *
     * @param cachedCredentials
     *        The credentials to remove (not <code>null</code>)
     * @throws RuntimeException
     *         if the credentials store is not writable
     * @return <code>true</code> if the credentials were successfully removed,
     *         <code>false</code> otherwise
     */
    boolean removeCredentials(CachedCredentials cachedCredentials);

    /**
     * Removes the credentials for the given URI.
     * <p>
     * Users should ensure that this credentials provider is writable by calling
     * the {@link #canWrite()} method for calling this one.
     *
     * @param cachedCredentials
     *        The credentials to remove (not <code>null</code>)
     * @throws RuntimeException
     *         if the credentials store is not writable
     * @return <code>true</code> if the credentials were successfully removed,
     *         <code>false</code> otherwise
     */
    boolean removeCredentials(URI uri);
}
