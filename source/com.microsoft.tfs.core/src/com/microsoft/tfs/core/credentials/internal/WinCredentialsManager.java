// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.credentials.internal;

import java.net.URI;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.core.credentials.CachedCredentials;
import com.microsoft.tfs.core.credentials.CredentialsManager;
import com.microsoft.tfs.core.util.URIUtils;
import com.microsoft.tfs.jni.WinCredential;
import com.microsoft.tfs.jni.WinCredentialUtils;
import com.microsoft.tfs.util.Check;

/**
 * An {@link CredentialsManager} that can read and write passwords securely in
 * Windows Credentials Manager.
 */
public class WinCredentialsManager implements CredentialsManager {
    public WinCredentialsManager() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getUIMechanismName() {
        return Messages.getString("WinCredentialsManager.CredentialManager"); //$NON-NLS-1$
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canWrite() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isSecure() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CachedCredentials[] getCredentials() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CachedCredentials getCredentials(final URI serverURI) {
        Check.notNull(serverURI, "serverURI"); //$NON-NLS-1$

        // Reduce to scheme, host, port
        final URI uri = URIUtils.removePathAndQueryParts(serverURI);

        final WinCredential credential = WinCredentialUtils.getInstance().findCredential(newWinCredentialFromURI(uri));

        if (credential == null) {
            return null;
        }

        final String username = credential.getAccountName();
        final String password = credential.getPassword();

        return new CachedCredentials(uri, username, password);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean setCredentials(CachedCredentials cachedCredentials) {
        Check.notNull(cachedCredentials, "cachedCredentials"); //$NON-NLS-1$
        Check.notNull(cachedCredentials.getURI(), "cachedCredentials.getURI()"); //$NON-NLS-1$

        // Reduce to scheme, host, port
        final URI serverURI = URIUtils.removePathAndQueryParts(cachedCredentials.getURI());
        cachedCredentials =
            new CachedCredentials(serverURI, cachedCredentials.getUsername(), cachedCredentials.getPassword());

        final WinCredential credential = newWinCredentialFromURI(serverURI);
        credential.setAccountName(cachedCredentials.getUsername());

        if (cachedCredentials.getPassword() != null) {
            credential.setPassword(cachedCredentials.getPassword());
        }

        return WinCredentialUtils.getInstance().storeCredential(credential);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean removeCredentials(final CachedCredentials cachedCredentials) {
        Check.notNull(cachedCredentials, "cachedCredentials"); //$NON-NLS-1$
        Check.notNull(cachedCredentials.getURI(), "cachedCredentials.getURI()"); //$NON-NLS-1$

        return removeCredentials(cachedCredentials.getURI());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean removeCredentials(final URI uri) {
        Check.notNull(uri, "uri"); //$NON-NLS-1$

        // Reduce to scheme, host, port
        final URI serverURI = URIUtils.removePathAndQueryParts(uri);

        final WinCredential credential = newWinCredentialFromURI(serverURI);

        return WinCredentialUtils.getInstance().eraseCredential(credential);
    }

    /**
     * @param uri
     *        the {@link URI} to use; should already have path and query parts
     *        removed ({@link URIUtils#removePathAndQueryParts(URI)}) (must not
     *        be <code>null</code>)
     */
    private static WinCredential newWinCredentialFromURI(final URI uri) {
        Check.notNull(uri, "uri"); //$NON-NLS-1$

        final WinCredential credential = new WinCredential();

        if (uri.getHost() != null && uri.getHost().length() > 0) {
            credential.setServerUri(uri.getHost());
        }

        return credential;
    }
}
