// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.credentials;

import java.net.URI;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.UUID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.alm.auth.Authenticator;
import com.microsoft.alm.auth.PromptBehavior;
import com.microsoft.alm.auth.oauth.OAuth2Authenticator;
import com.microsoft.alm.auth.pat.VstsPatAuthenticator;
import com.microsoft.alm.secret.Token;
import com.microsoft.alm.secret.TokenPair;
import com.microsoft.alm.secret.TokenType;
import com.microsoft.alm.secret.VsoTokenScope;
import com.microsoft.alm.storage.InsecureInMemoryStore;
import com.microsoft.alm.storage.SecretStore;
import com.microsoft.tfs.client.common.Messages;
import com.microsoft.tfs.client.common.config.CommonClientConnectionAdvisor;
import com.microsoft.tfs.core.TFSConfigurationServer;
import com.microsoft.tfs.core.TFSConnection;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.credentials.CachedCredentials;
import com.microsoft.tfs.core.credentials.CredentialsManager;
import com.microsoft.tfs.core.httpclient.CookieCredentials;
import com.microsoft.tfs.core.httpclient.Credentials;
import com.microsoft.tfs.core.httpclient.JwtCredentials;
import com.microsoft.tfs.core.httpclient.PreemptiveUsernamePasswordCredentials;
import com.microsoft.tfs.core.httpclient.UsernamePasswordCredentials.PatCredentials;
import com.microsoft.tfs.core.util.URIUtils;
import com.microsoft.tfs.jni.helpers.LocalHost;
import com.microsoft.tfs.util.StringUtil;
import com.microsoft.visualstudio.services.account.AccountHttpClient;
import com.microsoft.visualstudio.services.delegatedauthorization.DelegatedAuthorizationHttpClient;
import com.microsoft.visualstudio.services.delegatedauthorization.model.SessionToken;

/**
 * Static methods to manipulate {@link SessionToken}s.
 *
 * @threadsafety thread-safe
 */

public abstract class CredentialsHelper {
    private static final Log log = LogFactory.getLog(CredentialsHelper.class);

    /**
     * Constants for OAuth2 Interactive Browser logon flow
     */
    private static final String CLIENT_ID = "97877f11-0fc6-4aee-b1ff-febb0519dd00"; //$NON-NLS-1$
    private static final String REDIRECT_URL = "https://java.visualstudio.com"; //$NON-NLS-1$

    private static CredentialsManager gitCredentialsManager =
        EclipseCredentialsManagerFactory.getGitCredentialsManager();
    final static SecretStore<TokenPair> accessTokenStore = new InsecureInMemoryStore<TokenPair>();
    final static SecretStore<Token> tokenStore = new EclipseTokenStore();

    public static void createAccountCodeAccessToken(final TFSConnection connection) {
        if (connection.isHosted() && !hasAccountCodeAccessToken(connection)) {
            final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss"); //$NON-NLS-1$
            dateFormat.setTimeZone(TimeZone.getTimeZone("UTC")); //$NON-NLS-1$

            final String tokenDisplayName = MessageFormat.format(
                PatCredentials.TOKEN_DESCRIPTION,
                connection.getAuthorizedAccountName(),
                LocalHost.getShortName(),
                dateFormat.format(new Date()));

            final TFSConnection vstsConnection = getVstsRootConnection(connection);
            final DelegatedAuthorizationHttpClient authorizationClient =
                new DelegatedAuthorizationHttpClient(vstsConnection);

            final UUID accountId = getAccountId(connection);
            final String pat =
                authorizationClient.createAccountCodeAccessToken(tokenDisplayName, accountId).getAlternateToken();

            final URI baseURI = connection.getBaseURI();
            gitCredentialsManager.setCredentials(new CachedCredentials(baseURI, pat)); // $NON-NLS-1$
        }
    }

    public static void refreshAccountCodeAccessToken(final TFSConnection connection) {
        if (connection.isHosted()) {
            final URI baseURI = connection.getBaseURI();
            final CachedCredentials cachedCredentials = gitCredentialsManager.getCredentials(baseURI);

            if (cachedCredentials.isPatCredentials()) {
                gitCredentialsManager.removeCredentials(cachedCredentials);
                createAccountCodeAccessToken(connection);
            }
        }

    }

    public static boolean hasAccountCodeAccessToken(final TFSConnection connection) {
        if (connection.isHosted()) {
            final URI baseURI = connection.getBaseURI();
            final CachedCredentials cachedCredentials = gitCredentialsManager.getCredentials(baseURI);

            if (cachedCredentials != null
                && cachedCredentials.isPatCredentials()
                && !StringUtil.isNullOrEmpty(cachedCredentials.getPassword())) {

                return true;
            }
        }

        return false;
    }

    public static boolean hasAlternateCredentials(final TFSConnection connection) {
        if (connection.isHosted()) {
            final URI baseURI = connection.getBaseURI();
            final CachedCredentials cachedCredentials = gitCredentialsManager.getCredentials(baseURI);

            if (cachedCredentials != null && cachedCredentials.isUsernamePasswordCredentials()) {
                return true;
            }
        }

        return false;
    }

    public static boolean isCredentialsValid(final TFSConnection connection, final Credentials credentials) {
        final URI baseURI = connection.getBaseURI();
        return isCredentialsValid(baseURI, credentials);
    }

    public static boolean isAccountCodeAccessTokenValid(final TFSConnection connection) {
        if (connection.isHosted()) {
            final URI baseURI = connection.getBaseURI();
            final CachedCredentials cachedCredentials = gitCredentialsManager.getCredentials(baseURI);

            if (cachedCredentials != null && cachedCredentials.isPatCredentials()) {
                final PatCredentials patCredentials = (PatCredentials) cachedCredentials.toCredentials();
                return isCredentialsValid(connection, PreemptiveUsernamePasswordCredentials.newFrom(patCredentials));
            }
        }

        return false;
    }

    public static UUID getAccountId(final TFSConnection connection) {
        if (connection instanceof TFSConfigurationServer) {
            return UUID.fromString(((TFSConfigurationServer) connection).getInstanceID().toString());
        } else {
            return getAccountId(((TFSTeamProjectCollection) connection).getConfigurationServer());
        }
    }

    public static Credentials getVstsRootCredentials(final TFSConnection connection) {
        final Credentials currentCredentials = connection.getCredentials();

        if (currentCredentials instanceof CookieCredentials) {
            return ((CookieCredentials) currentCredentials).setDomain(URIUtils.VSTS_SUFFIX);
        } else {
            return currentCredentials;
        }
    }

    public static TFSConnection getVstsRootConnection(final TFSConnection connection) {
        final TFSConnection vstsConnection = new TFSTeamProjectCollection(
            URIUtils.VSTS_ROOT_URL,
            getVstsRootCredentials(connection),
            connection.getConnectionAdvisor());

        return vstsConnection;
    }

    public static Credentials getOAuthCredentials(final URI serverURI) {
        removeStaleOAuth2Token();

        final Authenticator authenticator;
        final Token token;

        if (serverURI != null) {
            log.debug("Interactively retrieving credential based on oauth2 flow for " + serverURI.toString()); //$NON-NLS-1$
            log.debug("Trying to persist credential, generating a PAT"); //$NON-NLS-1$

            authenticator = new VstsPatAuthenticator(CLIENT_ID, REDIRECT_URL, accessTokenStore, tokenStore);

            final String tokenKey =
                authenticator.getUriToKeyConversion().convert(serverURI, authenticator.getAuthType());
            removeStalePersonalAccessToken(tokenKey, serverURI);

            token = authenticator.getPersonalAccessToken(
                serverURI,
                VsoTokenScope.AllScopes,
                PatCredentials.USERNAME_FOR_CODE_ACCESS_PAT,
                PromptBehavior.AUTO);
        } else {
            log.debug("Interactively retrieving credential based on oauth2 flow for VSTS"); //$NON-NLS-1$
            log.debug("Do not try to persist, generating oauth2 token."); //$NON-NLS-1$

            authenticator = OAuth2Authenticator.getAuthenticator(CLIENT_ID, REDIRECT_URL, accessTokenStore);

            final TokenPair tokenPair = authenticator.getOAuth2TokenPair();
            token = tokenPair != null ? tokenPair.AccessToken : null;
        }

        if (token != null && token.Type != null && !StringUtil.isNullOrEmpty(token.Value)) {
            switch (token.Type) {
                case Personal:
                    return new PatCredentials(token.Value);
                case Access:
                    return new JwtCredentials(token.Value);
            }
        }

        log.warn(Messages.getString("CredentialsHelper.InteractiveAuthenticationFailedDetailedLog1")); //$NON-NLS-1$
        log.warn(Messages.getString("CredentialsHelper.InteractiveAuthenticationFailedDetailedLog2")); //$NON-NLS-1$
        log.warn(Messages.getString("CredentialsHelper.InteractiveAuthenticationFailedDetailedLog3")); //$NON-NLS-1$

        // Failed to get credential, return null
        return null;
    }

    private static void removeStaleOAuth2Token() {
        final Authenticator authenticator =
            OAuth2Authenticator.getAuthenticator(CLIENT_ID, REDIRECT_URL, accessTokenStore);

        final String tokenKey =
            authenticator.getUriToKeyConversion().convert(URIUtils.VSTS_ROOT_URL, authenticator.getAuthType());
        final TokenPair oauth2TokenPair = accessTokenStore.get(tokenKey);

        if (oauth2TokenPair != null && oauth2TokenPair.AccessToken != null) {
            final String token = oauth2TokenPair.AccessToken.Value;

            if (!StringUtil.isNullOrEmpty(token) && !isOAuth2TokenValid(token)) {
                accessTokenStore.delete(tokenKey);
            }
        }
    }

    private static void removeStalePersonalAccessToken(final String tokenKey, final URI serverURI) {
        final Token token = tokenStore.get(tokenKey);

        if (token != null && !StringUtil.isNullOrEmpty(token.Value) && !isAccessTokenValid(token.Value, serverURI)) {
            tokenStore.delete(tokenKey);
        }
    }

    private static boolean isOAuth2TokenValid(final String token) {
        final URI serverURI = URIUtils.VSTS_ROOT_URL;
        final JwtCredentials credentials = new JwtCredentials(token);
        return isCredentialsValid(serverURI, credentials);
    }

    private static boolean isAccessTokenValid(String token, URI serverURI) {
        final PatCredentials patCredentials = new PatCredentials(token);
        return isCredentialsValid(serverURI, PreemptiveUsernamePasswordCredentials.newFrom(patCredentials));
    }

    private static boolean isCredentialsValid(URI baseURI, Credentials credentials) {
        final TFSTeamProjectCollection rootConnection = new TFSTeamProjectCollection(
            baseURI,
            credentials,
            new CommonClientConnectionAdvisor(Locale.getDefault(), TimeZone.getDefault()));
        final AccountHttpClient client = new AccountHttpClient(rootConnection);

        return client.checkConnection();
    }

    private static class EclipseTokenStore implements SecretStore<Token> {
        /**
         * {@inheritDoc}
         */
        @Override
        public boolean add(String key, Token token) {
            final URI serverURI = URIUtils.newURI(key.split(":", 2)[1]); //$NON-NLS-1$
            return gitCredentialsManager.setCredentials(new CachedCredentials(serverURI, token.Value));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean delete(String key) {
            final URI serverURI = URIUtils.newURI(key.split(":", 2)[1]); //$NON-NLS-1$
            return gitCredentialsManager.removeCredentials(serverURI);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Token get(String key) {
            final URI serverURI = URIUtils.newURI(key.split(":", 2)[1]); //$NON-NLS-1$
            CachedCredentials cachedCredentials = gitCredentialsManager.getCredentials(serverURI);
            if (cachedCredentials != null) {
                return new Token(cachedCredentials.getPassword(), TokenType.Personal);
            } else {
                return null;
            }

        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isSecure() {
            return true;
        }
    }
}
