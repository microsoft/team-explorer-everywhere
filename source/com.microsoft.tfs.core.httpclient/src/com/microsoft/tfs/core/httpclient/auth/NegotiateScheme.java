// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

/**
 * An implementation of the Negotiate authentication scheme.
 */

package com.microsoft.tfs.core.httpclient.auth;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.core.httpclient.Credentials;
import com.microsoft.tfs.core.httpclient.DefaultNTCredentials;
import com.microsoft.tfs.core.httpclient.HttpMethod;
import com.microsoft.tfs.core.httpclient.UsernamePasswordCredentials;
import com.microsoft.tfs.core.httpclient.util.EncodingUtil;
import com.microsoft.tfs.jni.NegotiateEngine;
import com.microsoft.tfs.jni.NegotiateEngine.NegotiateClient;
import com.microsoft.tfs.jni.helpers.LocalHost;

public class NegotiateScheme extends AuthorizationHeaderScheme implements AuthScheme {
    private static final Log LOG = LogFactory.getLog(NegotiateScheme.class);

    /* The status of this scheme */
    private static final int STATUS_NONE = 0;
    private static final int STATUS_INITIATED = 1;
    private static final int STATUS_EXCHANGING = 2;
    private static final int STATUS_COMPLETE = 3;
    private static final int STATUS_ERROR = 4;

    /* The current status of this scheme */
    private int status = STATUS_NONE;

    /* The challenge received from the server */
    private byte[] inputToken = null;

    /* The authentication engine */
    private NegotiateClient negotiateClient;

    /* The local host name of this machine */
    private final String localHostname = LocalHost.getShortName();

    /* The Negotiate engine */
    public NegotiateScheme() {
        super();
    }

    @Override
    public String getSchemeName() {
        return "negotiate";
    }

    public static boolean isSupported() {
        try {
            return NegotiateEngine.getInstance().isAvailable();
        } catch (final Exception e) {
            LOG.debug("Negotiate authentication not supported", e);
            return false;
        }
    }

    @Override
    public boolean supportsCredentials(final Credentials credentials) {
        if (credentials == null) {
            return false;
        }

        return supportsCredentials(credentials.getClass());
    }

    public static boolean supportsCredentials(final Class<?> credentialClass) {
        if (credentialClass == null || !isSupported()) {
            return false;
        }

        if (credentialClass.equals(DefaultNTCredentials.class)) {
            return NegotiateEngine.getInstance().supportsCredentialsDefault();
        } else if (credentialClass.equals(UsernamePasswordCredentials.class)) {
            return NegotiateEngine.getInstance().supportsCredentialsSpecified();
        }

        return false;
    }

    public static String getDefaultCredentials() {
        if (!isSupported()) {
            return null;
        }

        return NegotiateEngine.getInstance().getCredentialsDefault();
    }

    /**
     * There is no realm associated with Negotiate authentication, thus we
     * return null.
     *
     * @return null
     * @see com.microsoft.tfs.core.httpclient.auth.AuthScheme#getRealm()
     */
    public String getRealm() {
        return null;
    }

    /**
     * This authentication scheme does not have parameters, thus we always
     * return null.
     *
     * @return null
     * @see com.microsoft.tfs.core.httpclient.auth.AuthScheme#getParameter(java.lang.String)
     */
    @Override
    public String getParameter(final String name) {
        return null;
    }

    /**
     * NTLM2 is complete once we've delivered our authentication response (the
     * Type3 message) or we've failed, whichever comes first.
     *
     * @return true if authentication is complete, false if there are more steps
     * @see com.microsoft.tfs.core.httpclient.auth.AuthScheme#isComplete()
     */
    @Override
    public boolean isComplete() {
        return (status == STATUS_COMPLETE || status == STATUS_ERROR);
    }

    /**
     * NTLM2 provides authorization on a per-connection basis instead of the
     * usual per-request basis.
     *
     * @return true
     * @see com.microsoft.tfs.core.httpclient.auth.AuthScheme#isConnectionBased()
     */
    @Override
    public boolean isConnectionBased() {
        return true;
    }

    @Override
    public void processChallenge(final String challenge) throws MalformedChallengeException {
        /*
         * The first message received is simply "Negotiate", asking us for a
         * request
         */
        if (status == STATUS_NONE && challenge.equals("Negotiate")) {
            /*
             * We may be called for retry at an arbitrary time. If that's the
             * case, tear down any existing objects
             */
            if (negotiateClient != null) {
                try {
                    negotiateClient.dispose();
                } catch (final Exception e) {
                }
            }

            negotiateClient = null;
            inputToken = null;

            status = STATUS_INITIATED;
        }
        /* The server has responded with a challenge to our request */
        else if (status == STATUS_EXCHANGING && challenge.startsWith("Negotiate ")) {
            inputToken = Base64.decodeBase64(EncodingUtil.getAsciiBytes(challenge.substring(10)));
        }
        /* Otherwise, we've failed authentication (this gets called again) */
        else if (challenge.startsWith("Negotiate")) {
            status = STATUS_ERROR;
        }
        /* Totally bogus string */
        else {
            throw new MalformedChallengeException("Unknown challenge: " + challenge);
        }
    }

    @Override
    protected String authenticate(final AuthScope authscope, final Credentials credentials, final HttpMethod method)
        throws AuthenticationException {
        if (authscope == null || credentials == null || method == null) {
            throw new AuthenticationException("Invalid authentication usage");
        }

        if (!supportsCredentials(credentials)) {
            throw new AuthenticationException("Credential type unsupported");
        }

        byte[] token;

        try {
            if (status == STATUS_INITIATED && negotiateClient == null && inputToken == null) {
                negotiateClient = (NegotiateClient) NegotiateEngine.getInstance().newClient();
                negotiateClient.setTarget("http@" + authscope.getHost().toUpperCase());

                if (credentials instanceof DefaultNTCredentials) {
                    negotiateClient.setCredentialsDefault();
                } else if (credentials instanceof UsernamePasswordCredentials) {
                    final WindowsUser user = new WindowsUser(((UsernamePasswordCredentials) credentials).getUsername());

                    negotiateClient.setCredentialsSpecified(
                        user.getUsername(),
                        user.getDomain(),
                        ((UsernamePasswordCredentials) credentials).getPassword());

                    negotiateClient.setLocalhost(localHostname);
                } else {
                    throw new AuthenticationException("Unsupported credential type");
                }
            }
            /* Sanity check */
            else if (status != STATUS_EXCHANGING || negotiateClient == null || inputToken == null) {
                LOG.error("Negotiate authenticate called in invalid state " + status);

                status = STATUS_ERROR;
                throw new AuthenticationException("Negotiate Authentication Routines Used Out of Order");
            }

            token = negotiateClient.getToken(inputToken);

            if (negotiateClient.isComplete()) {
                status = STATUS_COMPLETE;

                /* Clean up */
                negotiateClient.dispose();

                negotiateClient = null;
                inputToken = null;
            } else {
                status = STATUS_EXCHANGING;
            }
        } catch (final com.microsoft.tfs.jni.AuthenticationEngine.AuthenticationException e) {
            LOG.error("Negotiate failure: " + e.getMessage());

            status = STATUS_ERROR;
            return null;
        }

        if (token == null || token.length == 0) {
            status = STATUS_ERROR;
            throw new AuthenticationException("Negotiate Scheme did not provided token");
        }

        final String tokenBase64 = EncodingUtil.getAsciiString(Base64.encodeBase64(token));

        return "Negotiate " + tokenBase64;
    }
}
