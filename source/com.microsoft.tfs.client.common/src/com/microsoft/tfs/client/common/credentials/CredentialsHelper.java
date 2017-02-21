// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.credentials;

import java.io.IOException;
import java.net.URI;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.alm.auth.Authenticator;
import com.microsoft.alm.auth.HttpClientFactory;
import com.microsoft.alm.auth.PromptBehavior;
import com.microsoft.alm.auth.oauth.DeviceFlowResponse;
import com.microsoft.alm.auth.oauth.Global;
import com.microsoft.alm.auth.oauth.OAuth2Authenticator;
import com.microsoft.alm.auth.pat.VstsPatAuthenticator;
import com.microsoft.alm.client.TeeClientHandler;
import com.microsoft.alm.helpers.Action;
import com.microsoft.alm.helpers.HttpClient;
import com.microsoft.alm.helpers.HttpResponse;
import com.microsoft.alm.helpers.StringContent;
import com.microsoft.alm.secret.Token;
import com.microsoft.alm.secret.TokenPair;
import com.microsoft.alm.secret.TokenType;
import com.microsoft.alm.secret.VsoTokenScope;
import com.microsoft.alm.storage.InsecureInMemoryStore;
import com.microsoft.alm.storage.SecretStore;
import com.microsoft.alm.visualstudio.services.account.client.AccountHttpClient;
import com.microsoft.alm.visualstudio.services.delegatedauthorization.SessionToken;
import com.microsoft.tfs.client.common.Messages;
import com.microsoft.tfs.client.common.config.CommonClientConnectionAdvisor;
import com.microsoft.tfs.core.TFSConnection;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.credentials.CachedCredentials;
import com.microsoft.tfs.core.credentials.CredentialsManager;
import com.microsoft.tfs.core.httpclient.Credentials;
import com.microsoft.tfs.core.httpclient.Header;
import com.microsoft.tfs.core.httpclient.HttpMethodBase;
import com.microsoft.tfs.core.httpclient.HttpStatus;
import com.microsoft.tfs.core.httpclient.JwtCredentials;
import com.microsoft.tfs.core.httpclient.PreemptiveUsernamePasswordCredentials;
import com.microsoft.tfs.core.httpclient.UsernamePasswordCredentials.PatCredentials;
import com.microsoft.tfs.core.httpclient.methods.EntityEnclosingMethod;
import com.microsoft.tfs.core.httpclient.methods.GetMethod;
import com.microsoft.tfs.core.httpclient.methods.HeadMethod;
import com.microsoft.tfs.core.httpclient.methods.PostMethod;
import com.microsoft.tfs.core.httpclient.methods.StringRequestEntity;
import com.microsoft.tfs.core.util.URIUtils;
import com.microsoft.tfs.jni.helpers.LocalHost;
import com.microsoft.tfs.util.StringUtil;

/**
 * Static methods to manipulate {@link SessionToken}s.
 *
 * @threadsafety thread-safe
 */

public abstract class CredentialsHelper {
    private final static Log log = LogFactory.getLog(CredentialsHelper.class);

    /**
     * Constants for OAuth2 Interactive Browser logon flow
     */
    private final static String CLIENT_ID = "97877f11-0fc6-4aee-b1ff-febb0519dd00"; //$NON-NLS-1$
    private final static String REDIRECT_URL = "https://java.visualstudio.com"; //$NON-NLS-1$

    private final static CredentialsManager gitCredentialsManager =
        EclipseCredentialsManagerFactory.getGitCredentialsManager();
    private final static SecretStore<TokenPair> accessTokenStore = new InsecureInMemoryStore<TokenPair>();
    private final static SecretStore<Token> tokenStore = new EclipseTokenStore();

    public static void refreshCredentialsForGit(final TFSConnection connection) {
        final URI baseURI = connection.getBaseURI();
        final CachedCredentials currentCredentials = new CachedCredentials(baseURI, connection.getCredentials());

        if (currentCredentials.isCookieCredentials() || currentCredentials.isNtlmCredentials()) {
            // The current credentials are not of the UsernamePassword type.
            // We cannot use them for Git.
            return;
        }

        final CachedCredentials cachedCredentials = gitCredentialsManager.getCredentials(baseURI);

        if (cachedCredentials == null) {
            // No credentials are cached for Git.
            // Let's use the current ones. They might be either PAT or
            // Alternative (on hosted)/Basic (on prem.)
            gitCredentialsManager.setCredentials(currentCredentials);
            return;
        }

        if (cachedCredentials.equals(currentCredentials)) {
            // The credentials haven't changed.
            // No need to refresh.
            return;
        }

        if (cachedCredentials.isPatCredentials() == currentCredentials.isPatCredentials()) {
            // The credentials are changed and are of the same type, i.e
            // either both PAT, or both Alternative/Basic. Let's refresh the
            // cached credentials.
            gitCredentialsManager.setCredentials(currentCredentials);
            return;
        }

        log.info("The type of cached credentials does not match to the one of the current credentials."); //$NON-NLS-1$
        log.info("The user has to clean up the cached credentials explicitly."); //$NON-NLS-1$
    }

    public static Credentials getOAuthCredentials(
        final URI serverURI,
        final JwtCredentials accessToken,
        final Action<DeviceFlowResponse> callback) {
        removeStaleOAuth2Token();

        final Authenticator authenticator;
        final OAuth2Authenticator oauth2Authenticator =
            OAuth2Authenticator.getAuthenticator(CLIENT_ID, REDIRECT_URL, accessTokenStore, callback);
        final Token token;

        final AuthLibHttpClientFactory authLibHttpClientFactory = new AuthLibHttpClientFactory();
        Global.setHttpClientFactory(authLibHttpClientFactory);

        if (serverURI != null) {
            log.debug("Interactively retrieving credential based on oauth2 flow for " + serverURI.toString()); //$NON-NLS-1$
            log.debug("Trying to persist credential, generating a PAT"); //$NON-NLS-1$

            authenticator = new VstsPatAuthenticator(oauth2Authenticator, tokenStore);

            final String tokenKey =
                authenticator.getUriToKeyConversion().convert(serverURI, authenticator.getAuthType());
            removeStalePersonalAccessToken(tokenKey, serverURI);

            final TokenPair oauth2Token =
                (accessToken == null) ? null : new TokenPair(accessToken.getAccessToken(), "null"); //$NON-NLS-1$

            token = authenticator.getPersonalAccessToken(
                serverURI,
                VsoTokenScope.AllScopes,
                getAccessTokenDescription(serverURI.toString()),
                PromptBehavior.AUTO,
                oauth2Token);
        } else {
            log.debug("Interactively retrieving credential based on oauth2 flow for VSTS"); //$NON-NLS-1$
            log.debug("Do not try to persist, generating oauth2 token."); //$NON-NLS-1$

            authenticator = oauth2Authenticator;

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

        removeOAuth2Token(true);

        // Failed to get credential, return null
        return null;
    }

    private static void removeStaleOAuth2Token() {
        removeOAuth2Token(false);
    }

    public static void removeOAuth2Token(final boolean force) {
        final Authenticator authenticator =
            OAuth2Authenticator.getAuthenticator(CLIENT_ID, REDIRECT_URL, accessTokenStore);

        final String tokenKey =
            authenticator.getUriToKeyConversion().convert(URIUtils.VSTS_ROOT_URL, authenticator.getAuthType());
        final TokenPair oauth2TokenPair = accessTokenStore.get(tokenKey);

        if (oauth2TokenPair != null && oauth2TokenPair.AccessToken != null) {
            final String token = oauth2TokenPair.AccessToken.Value;

            if (force || !isOAuth2TokenValid(token)) {
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
        if (StringUtil.isNullOrEmpty(token)) {
            return false;
        } else {
            final URI serverURI = URIUtils.VSTS_ROOT_URL;
            final JwtCredentials credentials = new JwtCredentials(token);

            return isCredentialsValid(serverURI, credentials);
        }
    }

    private static boolean isAccessTokenValid(String token, URI serverURI) {
        final PatCredentials patCredentials = new PatCredentials(token);
        return isCredentialsValid(serverURI, PreemptiveUsernamePasswordCredentials.newFrom(patCredentials));
    }

    private static boolean isCredentialsValid(URI baseURI, Credentials credentials) {

        /*
         * At this point we do not have any connection which HTTPClient we might
         * use to create a TeeClientHandler. Let's create a fake one. We do not
         * use the connection we create here as a real TFSTeamProjectColection.
         * We only use this fake connection object as a source of an HTTPClient
         * configured to use the VSTS credentials provided.
         */
        final TFSTeamProjectCollection rootConnection = new TFSTeamProjectCollection(
            baseURI,
            credentials,
            new CommonClientConnectionAdvisor(Locale.getDefault(), TimeZone.getDefault()));
        final AccountHttpClient client =
            new AccountHttpClient(new TeeClientHandler(rootConnection.getHTTPClient()), baseURI);

        try {
            return client.checkConnection();
        } finally {
            /*
             * We didn't use any features of the vstsConnection but the
             * HTTPClient. However to release all resources and the
             * infrastructure created for the connection (e.g.
             * ShoultDownManager, HTTPClientReference, Service Clients, etc.),
             * we still should close this connection when leaving the try-catch
             * block.
             */
            try {
                rootConnection.close();
            } catch (final Exception e) {
                log.error("Absolutelly unexpected error while closing not opened connection", e); //$NON-NLS-1$
            }
        }
    }

    private static String getAccessTokenDescription(final String uri) {
        final String tokenDescription =
            MessageFormat.format(PatCredentials.TOKEN_DESCRIPTION, uri, LocalHost.getShortName());

        return tokenDescription;
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

    private static class AuthLibHttpClientImpl implements HttpClient {

        final private com.microsoft.tfs.core.httpclient.HttpClient apacheClient;
        final private Map<String, String> headers;

        public AuthLibHttpClientImpl() {
            final TFSTeamProjectCollection rootConnection = new TFSTeamProjectCollection(
                URIUtils.VSTS_ROOT_URL,
                null,
                new CommonClientConnectionAdvisor(Locale.getDefault(), TimeZone.getDefault()));

            apacheClient = rootConnection.getHTTPClient();
            headers = new HashMap<String, String>();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Map<String, String> getHeaders() {
            return headers;
        }

        private String getResponseText(final HttpMethodBase request) throws IOException {
            final int statusCode = apacheClient.executeMethod(request);
            final String responseText = request.getResponseBodyAsString();

            if (HttpStatus.isSuccessFamily(statusCode)) {
                return responseText;
            }

            throw new IOException(HttpStatus.getStatusText(statusCode) + ":" + responseText); //$NON-NLS-1$
        }

        /**
         * Timeout is ignored
         */
        @Override
        public String getGetResponseText(final URI uri, final int timeout) throws IOException {
            return getGetResponseText(uri);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getGetResponseText(final URI uri) throws IOException {
            final HttpMethodBase request = new GetMethod(uri.toString());
            addHeader(request);

            return getResponseText(request);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getHeaderField(final URI uri, final String header) throws IOException {
            final HttpMethodBase request = new HeadMethod(uri.toString());
            addHeader(request);

            apacheClient.executeMethod(request);

            final Header[] headers = request.getResponseHeaders(header);
            if (headers.length > 0) {
                final Header responseHeader = headers[0];
                return responseHeader.getValue();
            }

            return null;
        }

        private EntityEnclosingMethod createPostMethod(final URI uri, final StringContent content) {
            final EntityEnclosingMethod request = new PostMethod(uri.toString());

            request.setRequestEntity(new StringRequestEntity(content.getContent()));
            getHeaders().putAll(content.Headers);
            addHeader(request);

            return request;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public HttpResponse getPostResponse(final URI uri, final StringContent content) throws IOException {
            final EntityEnclosingMethod request = createPostMethod(uri, content);

            final int statusCode = apacheClient.executeMethod(request);

            String responseOut = null;
            String responseError = null;

            if (HttpStatus.isSuccessFamily(statusCode)) {
                responseOut = request.getResponseBodyAsString();
            } else {
                responseError = request.getResponseBodyAsString();
            }

            HttpResponse httpResponse = new HttpResponse();
            httpResponse.status = statusCode;
            httpResponse.errorText = responseError;
            httpResponse.responseText = responseOut;

            return httpResponse;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getPostResponseText(URI uri, StringContent content) throws IOException {
            final EntityEnclosingMethod request = createPostMethod(uri, content);

            return getResponseText(request);
        }

        private void addHeader(final HttpMethodBase request) {
            for (final Map.Entry<String, String> entry : headers.entrySet()) {
                request.setRequestHeader(entry.getKey(), entry.getValue());
            }
        }
    }

    private static class AuthLibHttpClientFactory extends HttpClientFactory {
        @Override
        public HttpClient createHttpClient() {
            return new AuthLibHttpClientImpl();
        }
    }
}
