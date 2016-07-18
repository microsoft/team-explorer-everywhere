// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.credentials.internal;

import java.net.URI;

import com.microsoft.alm.secret.Credential;
import com.microsoft.alm.secret.Secret;
import com.microsoft.alm.storage.posix.GnomeKeyringBackedCredentialStore;
import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.core.credentials.CachedCredentials;
import com.microsoft.tfs.core.credentials.CredentialsManager;
import com.microsoft.tfs.core.util.URIUtils;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.StringUtil;

public class GnomeKeyringCredentialsManager implements CredentialsManager {

    private final static GnomeKeyringBackedCredentialStore gnomeKeyringStore;

    static {
        if (GnomeKeyringBackedCredentialStore.isGnomeKeyringSupported()) {
            gnomeKeyringStore = new GnomeKeyringBackedCredentialStore();
        } else {
            gnomeKeyringStore = null;
        }
    }

    public static boolean isGnomeKeyringSupported() {
        return gnomeKeyringStore != null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getUIMechanismName() {
        return Messages.getString("GnomeKeyringCredentialsManager.GnomeKeyring"); //$NON-NLS-1$
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
    public CachedCredentials getCredentials(URI serverURI) {
        Check.notNull(serverURI, "serverURI"); //$NON-NLS-1$

        // Reduce to scheme, host, port
        serverURI = URIUtils.removePathAndQueryParts(serverURI);

        if (gnomeKeyringStore != null) {
            final Credential cred = gnomeKeyringStore.get(getKey(serverURI));

            if (cred != null && !(StringUtil.isNullOrEmpty(cred.Password) || StringUtil.isNullOrEmpty(cred.Username))) {
                return new CachedCredentials(serverURI, cred.Username, cred.Password);
            }
        }

        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean setCredentials(CachedCredentials cachedCredentials) {
        Check.notNull(cachedCredentials, "cachedCredentials"); //$NON-NLS-1$
        Check.notNull(cachedCredentials.getURI(), "cachedCredentials.getURI()"); //$NON-NLS-1$

        if (gnomeKeyringStore == null) {
            return false;
        }

        // Reduce to scheme, host, port
        final URI serverURI = URIUtils.removePathAndQueryParts(cachedCredentials.getURI());
        cachedCredentials =
            new CachedCredentials(serverURI, cachedCredentials.getUsername(), cachedCredentials.getPassword());

        final Credential cred = new Credential(cachedCredentials.getUsername(), cachedCredentials.getPassword());

        return gnomeKeyringStore.add(getKey(serverURI), cred);
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

        if (gnomeKeyringStore == null) {
            return false;
        }

        // Reduce to scheme, host, port
        final URI serverURI = URIUtils.removePathAndQueryParts(uri);

        return gnomeKeyringStore.delete(getKey(serverURI));
    }

    private String getKey(final URI serverURI) {
        return Secret.DefaultUriNameConversion.convert(serverURI, "tee"); //$NON-NLS-1$
    }

}
