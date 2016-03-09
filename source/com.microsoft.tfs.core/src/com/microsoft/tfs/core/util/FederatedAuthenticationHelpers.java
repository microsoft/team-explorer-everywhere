// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.util;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.net.UnknownHostException;
import java.text.MessageFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.core.config.httpclient.HTTPClientFactory;
import com.microsoft.tfs.core.exceptions.ACSUnauthorizedException;
import com.microsoft.tfs.core.exceptions.HTTPProxyUnauthorizedException;
import com.microsoft.tfs.core.exceptions.mappers.VersionControlExceptionMapper;
import com.microsoft.tfs.core.httpclient.HttpClient;
import com.microsoft.tfs.core.httpclient.HttpException;
import com.microsoft.tfs.core.httpclient.HttpStatus;
import com.microsoft.tfs.core.httpclient.auth.AuthScope;
import com.microsoft.tfs.core.httpclient.cookie.CookiePolicy;
import com.microsoft.tfs.core.httpclient.methods.PostMethod;
import com.microsoft.tfs.core.ws.runtime.exceptions.ProxyUnauthorizedException;
import com.microsoft.tfs.core.ws.runtime.exceptions.TransportException;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.LocaleUtil;

public class FederatedAuthenticationHelpers {
    public final static Log log = LogFactory.getLog(FederatedAuthenticationHelpers.class);

    /**
     * Gets a WRAP access token from the specified ACS URI (the scheme, host,
     * and port are used; path is replaced with WRAP document path).
     * <p>
     * See http://msdn.microsoft.com/en-us/library/ee706734.aspx for WRAP
     * protocol specifics including restrictions on scope, name, and password
     * values.
     *
     * @param clientFactory
     *        an {@link HttpClient} factory (must not be <code>null</code>)
     * @param acsIssuerURI
     *        the URI to the Azure ACS authentication issuer (must not be
     *        <code>null</code>)
     * @param wrapScope
     *        the plaintext WRAP scope, <b>not</b> URL-encoded (must not be
     *        <code>null</code> or empty)
     * @param wrapName
     *        the plaintext WRAP username, <b>not</b> URL-encoded (must not be
     *        <code>null</code> or empty)
     * @param wrapPassword
     *        the plaintext WRAP password, <b>not</b> BASE-64 encoded (must not
     *        be <code>null</code>)
     * @return the WRAP access token, or <code>null</code> if it could not be
     *         retrieved because of a protocol or network error
     * @throws ACSUnauthorizedException
     *         if the WRAP token could not be retrieved because of an
     *         authorization problem to ACS
     * @throws HTTPProxyUnauthorizedException
     *         if an HTTP proxy denied the request for authorization reasons
     */
    public static String getWRAPAccessToken(
        final HTTPClientFactory clientFactory,
        final URI acsIssuerURI,
        final String wrapScope,
        final String wrapName,
        final String wrapPassword) throws ACSUnauthorizedException, HTTPProxyUnauthorizedException {
        Check.notNull(clientFactory, "clientFactory"); //$NON-NLS-1$
        Check.notNull(acsIssuerURI, "acsIssuerURI"); //$NON-NLS-1$

        Check.notNullOrEmpty(wrapScope, "wrapScope"); //$NON-NLS-1$
        Check.notNullOrEmpty(wrapName, "wrapName"); //$NON-NLS-1$
        Check.notNull(wrapPassword, "wrapPassword"); //$NON-NLS-1$

        final HttpClient httpClient = clientFactory.newHTTPClient();

        final URI postURI = URIUtils.resolve(acsIssuerURI, "/WRAPv0.9/"); //$NON-NLS-1$
        final PostMethod postMethod = new PostMethod(postURI.toString());

        /*
         * Disable host authentication because ACS will never require any and
         * the factory may have configured some.
         */
        httpClient.getState().clearCredentials();
        httpClient.getParams().setPreemptiveAuthenticationTypes(new Class[0]);

        /*
         * Ignore cookies, do not follow redirects, do not do authentication.
         */
        postMethod.getParams().setCookiePolicy(CookiePolicy.IGNORE_COOKIES);
        postMethod.setFollowRedirects(false);
        postMethod.setDoAuthentication(false);

        postMethod.addParameter("wrap_name", wrapName); //$NON-NLS-1$
        postMethod.addParameter("wrap_password", wrapPassword); //$NON-NLS-1$
        postMethod.addParameter("wrap_scope", wrapScope); //$NON-NLS-1$

        try {
            final int statusCode = httpClient.executeMethod(postMethod);
            log.trace(MessageFormat.format("WRAP post method status: {0}", HttpStatus.getStatusText(statusCode))); //$NON-NLS-1$

            switch (statusCode) {
                case HttpStatus.SC_OK:
                    return parseAccessTokenFromResponse(postMethod.getResponseBodyAsString());
                case HttpStatus.SC_PROXY_AUTHENTICATION_REQUIRED:
                    // Handle this case like the web service proxy layer for
                    // better error message to the user.
                    throw new HTTPProxyUnauthorizedException(
                        new ProxyUnauthorizedException(
                            httpClient.getHostConfiguration().getProxyHost(),
                            httpClient.getHostConfiguration().getProxyPort(),
                            httpClient.getState().getProxyCredentials(AuthScope.ANY)));
                default:
                    /*
                     * ACS includes extended error codes (including the HTTP
                     * response code) in the response body for many types of
                     * errors.
                     */
                    final String body = postMethod.getResponseBodyAsString();
                    log.warn(MessageFormat.format(
                        "ACS returned non-OK status {0}: {1}", //$NON-NLS-1$
                        HttpStatus.getStatusText(statusCode),
                        body));

                    throw new ACSUnauthorizedException(wrapName, getDetailMessage(body));
            }
        } catch (final HttpException e) {
            log.error(
                Messages.getString("FederatedAuthenticationHelpers.HTTPErrorGettingAccessToken", LocaleUtil.ROOT), //$NON-NLS-1$
                e);
            throw new TransportException(
                Messages.getString("FederatedAuthenticationHelpers.HTTPErrorGettingAccessToken"), //$NON-NLS-1$
                e);
        } catch (final UnknownHostException e) {
            /*
             * Let the core exception mapper transform this one correctly (they
             * all can handle TransportException caused by UnkownHostException,
             * so VC choice is arbitrary).
             */
            throw VersionControlExceptionMapper.map(new TransportException(e));
        } catch (final IOException e) {
            log.error(
                Messages.getString("FederatedAuthenticationHelpers.IOErrorGettingAccessToken", LocaleUtil.ROOT), //$NON-NLS-1$
                e);
            throw new TransportException(
                Messages.getString("FederatedAuthenticationHelpers.IOErrorGettingAccessToken"), //$NON-NLS-1$
                e);
        }
    }

    /**
     * Parses the detail message "ACS50012: Authentication failed. " from an ACS
     * error response body like:
     *
     * <pre>
     * Error:Code:403:SubCode:T0:Detail:ACS50012: Authentication failed. :TraceID:556a1fc6-3f3f-46f4-88ec-f8e865e6eca4:TimeStamp:2011-04-13 21:17:28Z
     * </pre>
     *
     * @return the (probably always English) detail code, or <code>null</code>
     *         if it was not found in the body
     */
    protected static String getDetailMessage(final String acsResponseBody) {
        Check.notNull(acsResponseBody, "acsResponseBody"); //$NON-NLS-1$

        /*
         * http://msdn.microsoft.com/en-us/library/ee706734.aspx explains the
         * format of this error response body, but it's not precise about the
         * syntax, only showing an example. After the ACS 2.0 (2011-04-07)
         * release, additional parameters followed the detail message, which
         * makes the syntax irregular with respect to key-value pairs (i.e.
         * sometimes singles or triples) and colon separators.
         *
         * This method only cares about the one or two fields that follow the
         * Detail token. If the token after Detail matches "ACSnnnnn", then we
         * want to get one more token (English message). If the token after
         * Detail doesn't match "ACSnnnnn", then we return just that token.
         */
        final String[] parts = acsResponseBody.split(":"); //$NON-NLS-1$

        for (int i = 0; i < parts.length; i++) {
            if (parts[i].equalsIgnoreCase("Detail")) //$NON-NLS-1$
            {
                String message =
                    Messages.getString("FederatedAuthenticationHelpers.DetailFieldPresentButMessageMissing"); //$NON-NLS-1$

                // One more part?
                if (i + 1 < parts.length) {
                    message = parts[i + 1];

                    // Not ACSnnnnn, so just return this part
                    if (message.matches("ACS\\d{5}") == false) //$NON-NLS-1$
                    {
                        return message;
                    }
                }

                // Two more parts?
                if (i + 2 < parts.length) {
                    message += ":" + parts[i + 2]; //$NON-NLS-1$
                }

                return message;
            }
        }

        return null;
    }

    /**
     * Parses the response body for the WRAP access token.
     *
     * @return the token, or <code>null</code> if it was not found in the
     *         response body
     */
    private static String parseAccessTokenFromResponse(final String response) {
        String accessToken = null;

        if (response != null) {
            log.trace(MessageFormat.format("WRAP post method response body: {0}", response)); //$NON-NLS-1$

            final String[] responsePairs = response.split("&"); //$NON-NLS-1$
            for (final String pair : responsePairs) {
                final String[] keyValue = pair.split("=", 2); //$NON-NLS-1$

                if (keyValue.length == 2 && keyValue[0].equalsIgnoreCase("wrap_access_token")) //$NON-NLS-1$
                {
                    // Token is HTTP form encoded
                    try {
                        accessToken = URLDecoder.decode(keyValue[1], "UTF-8"); //$NON-NLS-1$
                    } catch (final UnsupportedEncodingException e) {
                        log.error("Unsupported encoding while decoding access token", e); //$NON-NLS-1$
                        accessToken = null;
                        break;
                    }

                    log.trace(MessageFormat.format("Parsed access token: {0}", accessToken)); //$NON-NLS-1$
                    break;
                }
            }
        }

        if (accessToken == null) {
            log.warn(MessageFormat.format("Could not parse WRAP access token from response body: {0}", response)); //$NON-NLS-1$
        }

        return accessToken;
    }
}