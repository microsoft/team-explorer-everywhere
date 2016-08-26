// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.credentials.internal;

import java.net.URI;
import java.text.MessageFormat;
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
import com.microsoft.tfs.core.httpclient.Cookie;
import com.microsoft.tfs.core.httpclient.CookieCredentials;
import com.microsoft.tfs.core.httpclient.Credentials;
import com.microsoft.tfs.core.httpclient.UsernamePasswordCredentials;
import com.microsoft.tfs.core.httpclient.cookie.CookiePolicy;
import com.microsoft.tfs.core.httpclient.cookie.CookieSpec;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.StringUtil;

public class EclipseCredentialsManager implements CredentialsManager {
    public static final String GIT_PATH_PREFIX = "/GIT/"; //$NON-NLS-1$
    public static final String TEE_PATH_PREFIX = "/TEE/"; //$NON-NLS-1$

    private static final String USER_NAME = "user"; //$NON-NLS-1$
    private static final String PASSWORD = "password"; //$NON-NLS-1$
    private static final String HTTP_SCHEME = "http"; //$NON-NLS-1$
    private static final String HTTPS_SCHEME = "https"; //$NON-NLS-1$
    private static final String ENCODED_SLASH = "\\2f"; //$NON-NLS-1$
    private static final String COOKIE_PREFIX = "FedAuth"; //$NON-NLS-1$

    private static final Log log = LogFactory.getLog(EclipseCredentialsManager.class);

    private final String rootPathPrefix;
    private final ISecurePreferences preferences;
    private final PersistenceStoreProvider persistenceProvider;

    /*
     * The platform specific credentials manager is used for user name/password
     * credentials only. The Eclipse secure storage is used for Federated cookie
     * credentials
     */
    CredentialsManager platformCredentialsManager = null;

    public EclipseCredentialsManager(final String rootPathPrefix) {
        this(rootPathPrefix, null);
    }

    public EclipseCredentialsManager(final String rootPathPrefix, final PersistenceStoreProvider persistenceProvider) {
        this.rootPathPrefix = rootPathPrefix;
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
        // not used anywhere yet
        return null;
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

                /* Parse cookies according to RFC2109 */
                final CookieSpec cookieParser = CookiePolicy.getCookieSpec(CookiePolicy.RFC_2109);

                final String domain = serverURI.getHost();
                int port = serverURI.getPort();
                if (port < 0) {
                    if ("https".equalsIgnoreCase(serverURI.getScheme())) //$NON-NLS-1$
                    {
                        port = 443;
                    } else {
                        port = 80;
                    }
                }

                final String[] keys = node.keys();
                final List<Cookie> fedAuthCookies = new ArrayList<Cookie>();

                for (int k = 0; k < keys.length; k++) {
                    if (keys[k].startsWith(COOKIE_PREFIX)) {
                        final String cookieValue = node.get(keys[k], ""); //$NON-NLS-1$

                        try {
                            final Cookie[] cookies = cookieParser.parse(domain, port, "/", true, cookieValue); //$NON-NLS-1$

                            for (final Cookie cookie : cookies) {
                                if (cookie.getName().startsWith(COOKIE_PREFIX)) {
                                    /*
                                     * Setting the following property to true
                                     * makes cookies added to the HTTP headers
                                     * contain the attribute $Path=/ and thus a
                                     * semicolon between the cookie value and
                                     * this attribute.
                                     *
                                     * This is a workaround for a bug in cookie
                                     * processing on the server side: the cookie
                                     * values has to be appended with a
                                     * semicolon otherwise an error is reported
                                     * either by .NET or TFS (not clear yet by
                                     * which one exactly):
                                     *
                                     * "The input is not a valid Base-64 string
                                     * as it contains a non-base 64 character,
                                     * more than two padding characters, or an
                                     * illegal character among the padding
                                     * characters."
                                     */
                                    cookie.setPathAttributeSpecified(true);

                                    fedAuthCookies.add(cookie);
                                }
                            }
                        } catch (final Exception e) {
                            log.warn(MessageFormat.format("Could not parse authentication cookie {0}", cookieValue), e); //$NON-NLS-1$
                        }
                    }
                }

                if (fedAuthCookies.size() > 0) {
                    final Credentials credentials =
                        new CookieCredentials(fedAuthCookies.toArray(new Cookie[fedAuthCookies.size()]));

                    log.debug("Cookie credentials created"); //$NON-NLS-1$
                    return new CachedCredentials(serverURI, credentials);
                }
            } catch (final StorageException e) {
                log.error("Error reading credentials from the Eclipse secure store", e); //$NON-NLS-1$
            }
        }

        if (persistenceProvider == null) {
            return null;
        } else {
            return getPlatformCredentialsManager().getCredentials(serverURI);
        }
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
            credentials instanceof UsernamePasswordCredentials || credentials instanceof CookieCredentials,
            "credentials must be UsernamePasswordCredentials or CookieCredentials"); //$NON-NLS-1$

        if (credentials instanceof CookieCredentials || persistenceProvider == null) {
            try {
                final String nodePath = getNodePath(cachedCredentials.getURI());
                final ISecurePreferences node = preferences.node(nodePath);
                node.clear();

                if (credentials instanceof UsernamePasswordCredentials) {
                    node.put(USER_NAME, ((UsernamePasswordCredentials) credentials).getUsername(), false);
                    node.put(PASSWORD, ((UsernamePasswordCredentials) credentials).getPassword(), true);
                } else if (credentials instanceof CookieCredentials) {
                    final Cookie[] cookies = ((CookieCredentials) credentials).getCookies();
                    Check.notNullOrEmpty(cookies, "cookies"); //$NON-NLS-1$

                    for (int k = 0; k < cookies.length; k++) {
                        if (cookies[k].getName().startsWith(COOKIE_PREFIX)) {
                            node.put(
                                COOKIE_PREFIX + (k == 0 ? StringUtil.EMPTY : String.valueOf(k)),
                                cookies[k].toString(),
                                true);
                        }
                    }
                }

                node.flush();

                return true;
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
                return false;
            }
        } else {
            return getPlatformCredentialsManager().setCredentials(cachedCredentials);
        }
    }

    @Override
    public boolean removeCredentials(final URI uri) {
        Check.notNull(uri, "uri"); //$NON-NLS-1$

        final String nodePath = getNodePath(uri);
        if (preferences.nodeExists(nodePath)) {
            final ISecurePreferences node = preferences.node(nodePath);
            node.clear();
            node.removeNode();
            return true;
        } else if (persistenceProvider != null) {
            return getPlatformCredentialsManager().removeCredentials(uri);
        } else {
            return true;
        }
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

    public static class EclipseGitCredentialsManager extends EclipseCredentialsManager {

        public EclipseGitCredentialsManager() {
            super(GIT_PATH_PREFIX, null);
        }
    }

    public static class EclipseTeeCredentialsManager extends EclipseCredentialsManager {

        public EclipseTeeCredentialsManager(final PersistenceStoreProvider persistenceProvider) {
            super(TEE_PATH_PREFIX, persistenceProvider);
        }
    }
}
