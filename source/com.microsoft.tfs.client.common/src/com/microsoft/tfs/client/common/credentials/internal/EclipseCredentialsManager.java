// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.credentials.internal;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.equinox.security.storage.SecurePreferencesFactory;
import org.eclipse.equinox.security.storage.StorageException;

import com.microsoft.tfs.client.common.Messages;
import com.microsoft.tfs.core.config.persistence.PersistenceStoreProvider;
import com.microsoft.tfs.core.credentials.CachedCredentials;
import com.microsoft.tfs.core.credentials.CredentialsManager;
import com.microsoft.tfs.core.credentials.CredentialsManagerFactory;
import com.microsoft.tfs.core.httpclient.Credentials;
import com.microsoft.tfs.core.httpclient.UsernamePasswordCredentials;
import com.microsoft.tfs.core.util.URIUtils;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.StringUtil;

public class EclipseCredentialsManager implements CredentialsManager {
    public static final String GIT_PATH_PREFIX = "/GIT"; //$NON-NLS-1$

    private static final String USER_NAME = "user"; //$NON-NLS-1$
    private static final String PASSWORD = "password"; //$NON-NLS-1$
    private static final String HTTP_SCHEME = "http"; //$NON-NLS-1$
    private static final String HTTPS_SCHEME = "https"; //$NON-NLS-1$
    private static final String ENCODED_SLASH = "\\2f"; //$NON-NLS-1$
    private static final String SLASH = "/"; //$NON-NLS-1$

    private static final Log log = LogFactory.getLog(EclipseCredentialsManager.class);

    private final String rootPathPrefix;
    private final ISecurePreferences preferences;
    private final PersistenceStoreProvider persistenceProvider;

    /*
     * The platform specific credentials manager is used for user name/password
     * credentials only.
     */
    CredentialsManager platformCredentialsManager = null;

    public EclipseCredentialsManager(final PersistenceStoreProvider persistenceProvider) {
        Check.notNull(persistenceProvider, "persistenceProvider"); //$NON-NLS-1$

        this.rootPathPrefix = GIT_PATH_PREFIX;
        this.preferences = SecurePreferencesFactory.getDefault();
        this.persistenceProvider = persistenceProvider;
    }

    @Override
    public String getUIMechanismName() {
        return Messages.getString("EclipseCredentialsManager.Eclipse"); //$NON-NLS-1$
    }

    @Override
    public boolean canWrite() {
        return true;
    }

    @Override
    public boolean isSecure() {
        return true;
    }

    @Override
    public CachedCredentials[] getCredentials() {
        final List<CachedCredentials> credentials = new ArrayList<CachedCredentials>(100);
        final ISecurePreferences rootNode = preferences.node(rootPathPrefix);
        final String[] children = rootNode.childrenNames();

        for (final String child : children) {
            final String url = child.replace(ENCODED_SLASH, SLASH);

            try {
                final URI serverURI = URIUtils.newURI(url);

                final boolean isHttp;
                if (serverURI == null || StringUtil.isNullOrEmpty(serverURI.getScheme())) {
                    isHttp = false;
                } else if (serverURI.getScheme().equalsIgnoreCase("http") || //$NON-NLS-1$
                    serverURI.getScheme().equalsIgnoreCase("https")) { //$NON-NLS-1$
                    isHttp = true;
                } else {
                    isHttp = false;
                }

                if (isHttp) {
                    final CachedCredentials cachedCredentials = getCredentials(serverURI);

                    if (cachedCredentials != null) {
                        credentials.add(cachedCredentials);
                    }
                }
            } catch (final Exception e) {
                /*
                 * Log and ignore the exception. Maybe the node has been created not
                 * by the TEE plugin.
                 */
                log.warn("Ignoring the unexpected node " + url + " in the Eclipse credentials storage", e); //$NON-NLS-1$ //$NON-NLS-2$
            }
        }

        return credentials.toArray(new CachedCredentials[credentials.size()]);
    }

    @Override
    public CachedCredentials getCredentials(final URI serverURI) {
        final ISecurePreferences node = getSecureStorageNode(serverURI);

        if (node != null) {
            try {
                final String storedUserName = node.get(USER_NAME, ""); //$NON-NLS-1$
                final String password = node.get(PASSWORD, ""); //$NON-NLS-1$
                if (!StringUtil.isNullOrEmpty(storedUserName) && !StringUtil.isNullOrEmpty(password)) {
                    log.debug("User name & password credentials created"); //$NON-NLS-1$
                    return new CachedCredentials(serverURI, storedUserName, password);
                }
            } catch (final StorageException e) {
                log.error("Error reading credentials from the Eclipse secure store", e); //$NON-NLS-1$
            }
        }

        final CachedCredentials credentials = getPlatformCredentialsManager().getCredentials(serverURI);
        if (credentials != null) {
            final String credentialsType = credentials.isPatCredentials() ? "PAT" //$NON-NLS-1$
                : credentials.isUsernamePasswordCredentials() ? "User name & password" : "Unexpected"; //$NON-NLS-1$ //$NON-NLS-2$
            log.debug(credentialsType + " credentials created found in the platform credentials manager."); //$NON-NLS-1$
        }

        return credentials;
    }

    private ISecurePreferences getSecureStorageNode(final URI serverURI) {
        final String nodePath = getNodePath(serverURI);

        if (preferences.nodeExists(nodePath)) {
            log.debug("Saved credentials for " //$NON-NLS-1$
                + serverURI.toString()
                + " found in the Eclipse secure storage node " //$NON-NLS-1$
                + nodePath);
            return preferences.node(nodePath);
        }

        return null;
    }

    @Override
    public boolean setCredentials(final CachedCredentials cachedCredentials) {
        final Credentials credentials = cachedCredentials.toCredentials();
        Check.isTrue(
            credentials instanceof UsernamePasswordCredentials,
            "credentials must be UsernamePasswordCredentials"); //$NON-NLS-1$

        try {
            final String nodePath = getNodePath(cachedCredentials.getURI());
            final ISecurePreferences node = preferences.node(nodePath);
            node.clear();

            node.put(USER_NAME, ((UsernamePasswordCredentials) credentials).getUsername(), false);
            node.put(PASSWORD, ((UsernamePasswordCredentials) credentials).getPassword(), true);

            node.flush();
        } catch (final Exception e) {
            log.error("Error writing credentials to the Eclipse secure store", e); //$NON-NLS-1$
            final String nodePath = getNodePath(cachedCredentials.getURI());
            try {
                if (preferences.nodeExists(nodePath)) {
                    log.error("We'll remove the node " + nodePath + " in the credentials storage"); //$NON-NLS-1$ //$NON-NLS-2$
                    preferences.remove(nodePath);
                }
            } catch (final Exception ex) {
                log.error("Failed to remove the node", ex); //$NON-NLS-1$
            }
        }

        return getPlatformCredentialsManager().setCredentials(cachedCredentials);
    }

    @Override
    public boolean removeCredentials(final URI uri) {
        Check.notNull(uri, "uri"); //$NON-NLS-1$

        final String nodePath = getNodePath(uri);
        if (preferences.nodeExists(nodePath)) {
            final ISecurePreferences node = preferences.node(nodePath);
            node.clear();
            node.removeNode();
        }

        return getPlatformCredentialsManager().removeCredentials(uri);
    }

    @Override
    public boolean removeCredentials(final CachedCredentials cachedCredentials) {
        Check.notNull(cachedCredentials, "cachedCredentials"); //$NON-NLS-1$
        Check.notNull(cachedCredentials.getURI(), "cachedCredentials.getURI()"); //$NON-NLS-1$

        return removeCredentials(cachedCredentials.getURI());
    }

    private String getNodePath(final URI serverURI) {
        Check.notNull(serverURI, "serverURI"); //$NON-NLS-1$
        Check.isTrue(serverURI.isAbsolute(), "URI has to be absolute"); //$NON-NLS-1$
        Check.notNull(serverURI.getHost(), "serverURI.getHost()"); //$NON-NLS-1$

        final StringBuffer sb = new StringBuffer(rootPathPrefix);
        sb.append(SLASH);

        final String scheme = serverURI.getScheme();
        sb.append(scheme.toLowerCase());
        sb.append(':');

        sb.append(ENCODED_SLASH);
        sb.append(ENCODED_SLASH);

        final String host = serverURI.getHost().toLowerCase();
        sb.append(host);

        sb.append(':');
        if (serverURI.getPort() < 0) {
            if (scheme.equalsIgnoreCase(HTTP_SCHEME)) {
                sb.append("80"); //$NON-NLS-1$
            } else if (scheme.equalsIgnoreCase(HTTPS_SCHEME)) {
                sb.append("443"); //$NON-NLS-1$
            }
        } else {
            sb.append(serverURI.getPort());
        }

        return sb.toString();
    }

    private CredentialsManager getPlatformCredentialsManager() {
        Check.notNull(persistenceProvider, "persistenceProvider"); //$NON-NLS-1$

        /*
         * Windows always uses CredMan for credential storage, however this is
         * completely handled by the OS, so we can NullCredentialManager.
         *
         * Mac OS uses Keychain for credential storage.
         *
         * Unix uses PersistenceStoreCredentialsManager.
         */
        if (platformCredentialsManager == null) {
            platformCredentialsManager = CredentialsManagerFactory.getCredentialsManager(persistenceProvider);
        }

        return platformCredentialsManager;
    }
}
