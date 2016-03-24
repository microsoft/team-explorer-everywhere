// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.credentials.internal;

import java.io.UnsupportedEncodingException;
import java.net.URI;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.core.credentials.CachedCredentials;
import com.microsoft.tfs.core.credentials.CredentialsManager;
import com.microsoft.tfs.core.util.URIUtils;
import com.microsoft.tfs.jni.KeychainInternetPassword;
import com.microsoft.tfs.jni.KeychainProtocol;
import com.microsoft.tfs.jni.KeychainUtils;
import com.microsoft.tfs.util.Check;

/**
 * An {@link CredentialsManager} that can read and write passwords securely in
 * Mac OS Keychain. Since the keychain interface requires a username, we need to
 * store some information in the persistence store credentials provider.
 *
 * @threadsafety thread-safe
 */
public class KeychainCredentialsManager implements CredentialsManager {
    private static final Log log = LogFactory.getLog(KeychainCredentialsManager.class);

    public KeychainCredentialsManager() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getUIMechanismName() {
        return Messages.getString("KeychainCredentialsManager.Keychain"); //$NON-NLS-1$
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

        final KeychainInternetPassword keychainPassword =
            KeychainUtils.getInstance().findInternetPassword(newKeychainInternetPasswordFromURI(serverURI), true);

        if (keychainPassword == null) {
            return null;
        }

        final String username = keychainPassword.getAccountName();
        String password = null;

        if (keychainPassword.getPassword() != null && keychainPassword.getPassword().length > 0) {
            try {
                password = new String(keychainPassword.getPassword(), "UTF-8"); //$NON-NLS-1$
            } catch (final UnsupportedEncodingException e) {
                log.error("Could not convert byte array to plaintext", e); //$NON-NLS-1$
                return null;
            }
        }

        return new CachedCredentials(serverURI, username, password);
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

        final KeychainInternetPassword keychainPassword = newKeychainInternetPasswordFromURI(serverURI);
        keychainPassword.setAccountName(cachedCredentials.getUsername());

        if (cachedCredentials.getPassword() != null) {
            try {
                keychainPassword.setPassword(cachedCredentials.getPassword().getBytes("UTF-8")); //$NON-NLS-1$
            } catch (final UnsupportedEncodingException e) {
                log.error("Could not convert plaintext to byte array", e); //$NON-NLS-1$
                return false;
            }
        }

        return KeychainUtils.getInstance().addInternetPassword(keychainPassword, true);
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

        final KeychainInternetPassword keychainPassword = newKeychainInternetPasswordFromURI(serverURI);

        return KeychainUtils.getInstance().removeInternetPassword(keychainPassword, true);
    }

    /**
     * @param uri
     *        the {@link URI} to use; should already have path and query parts
     *        removed ({@link URIUtils#removePathAndQueryParts(URI)}) (must not
     *        be <code>null</code>)
     */
    private static KeychainInternetPassword newKeychainInternetPasswordFromURI(final URI uri) {
        Check.notNull(uri, "uri"); //$NON-NLS-1$

        final KeychainInternetPassword keychainPassword = new KeychainInternetPassword();

        /*
         * Compute the protocol.
         */
        if ("http".equalsIgnoreCase(uri.getScheme())) //$NON-NLS-1$
        {
            keychainPassword.setProtocol(KeychainProtocol.HTTP);
        } else if ("https".equalsIgnoreCase(uri.getScheme())) //$NON-NLS-1$
        {
            keychainPassword.setProtocol(KeychainProtocol.HTTPS);
        } else {
            keychainPassword.setProtocol(KeychainProtocol.ANY);
        }

        if (uri.getHost() != null && uri.getHost().length() > 0) {
            keychainPassword.setServerName(uri.getHost());
        }

        if (uri.getPort() > 0) {
            keychainPassword.setPort(uri.getPort());
        }

        if (uri.getPath() != null) {
            keychainPassword.setPath(uri.getPath());
        }

        return keychainPassword;
    }
}
