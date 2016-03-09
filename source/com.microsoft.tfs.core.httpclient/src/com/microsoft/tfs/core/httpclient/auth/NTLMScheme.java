// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

/**
 * An implementation of the NTLM authentication scheme.
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
import com.microsoft.tfs.jni.NTLMEngine;
import com.microsoft.tfs.jni.NTLMEngine.NTLMClient;
import com.microsoft.tfs.jni.helpers.LocalHost;

public class NTLMScheme extends AuthorizationHeaderScheme implements AuthScheme {
    private static final Log LOG = LogFactory.getLog(NTLMScheme.class);

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
    private NTLMClient ntlmClient;

    /* The local host name of this machine */
    private final String localHostname = LocalHost.getShortName();

    /* The NTLM engine */
    public NTLMScheme() {
        super();
    }

    @Override
    public String getSchemeName() {
        return "ntlm";
    }

    public static boolean isSupported() {
        try {
            return NTLMEngine.getInstance().isAvailable();
        } catch (final Exception e) {
            LOG.debug("NTLM authentication not supported", e);
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
            return NTLMEngine.getInstance().supportsCredentialsDefault();
        } else if (credentialClass.equals(UsernamePasswordCredentials.class)) {
            return NTLMEngine.getInstance().supportsCredentialsSpecified();
        }

        return false;
    }

    public static String getDefaultCredentials() {
        if (!isSupported()) {
            return null;
        }

        return NTLMEngine.getInstance().getCredentialsDefault();
    }

    /**
     * There is no realm associated with NTLM authentication, thus we return
     * null.
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
         * The first message received is simply "NTLM", asking us for a request
         */
        if (status == STATUS_NONE && challenge.equals("NTLM")) {
            /*
             * We may be called for retry at an arbitrary time. If that's the
             * case, tear down any existing objects
             */
            if (ntlmClient != null) {
                try {
                    ntlmClient.dispose();
                } catch (final Exception e) {
                }
            }

            ntlmClient = null;
            inputToken = null;

            status = STATUS_INITIATED;
        }
        /* The server has responded with a challenge to our request */
        else if (status == STATUS_EXCHANGING && challenge.startsWith("NTLM ")) {
            inputToken = Base64.decodeBase64(EncodingUtil.getAsciiBytes(challenge.substring(5)));
        }
        /* Otherwise, we've failed authentication (this gets called again) */
        else if (challenge.startsWith("NTLM")) {
            status = STATUS_ERROR;
        }
        /* Totally bogus string */
        else {
            throw new MalformedChallengeException("Unknown challenge: " + challenge);
        }
    }

    @Override
    public String authenticate(final AuthScope authscope, final Credentials credentials, final HttpMethod method)
        throws AuthenticationException {
        if (authscope == null || credentials == null || method == null) {
            throw new AuthenticationException("Invalid authentication usage");
        }

        if (!supportsCredentials(credentials)) {
            throw new AuthenticationException("Credential type unsupported");
        }

        byte[] token;

        try {
            if (status == STATUS_INITIATED && ntlmClient == null && inputToken == null) {
                ntlmClient = (NTLMClient) NTLMEngine.getInstance().newClient();
                ntlmClient.setTarget(authscope.getHost());

                if (credentials instanceof DefaultNTCredentials) {
                    ntlmClient.setCredentialsDefault();
                } else if (credentials instanceof UsernamePasswordCredentials) {
                    final WindowsUser user = new WindowsUser(((UsernamePasswordCredentials) credentials).getUsername());

                    ntlmClient.setCredentialsSpecified(
                        user.getUsername(),
                        user.getDomain(),
                        ((UsernamePasswordCredentials) credentials).getPassword());

                    ntlmClient.setLocalhost(localHostname);
                } else {
                    throw new AuthenticationException("Unsupported credential type");
                }
            }
            /* Sanity check */
            else if (status != STATUS_EXCHANGING || ntlmClient == null || inputToken == null) {
                LOG.error("NTLM authenticate called in invalid state " + status);

                status = STATUS_ERROR;
                throw new AuthenticationException("NTLM Authentication Routines Used Out of Order");
            }

            token = ntlmClient.getToken(inputToken);

            if (ntlmClient.isComplete()) {
                status = STATUS_COMPLETE;

                /* Clean up */
                ntlmClient.dispose();

                ntlmClient = null;
                inputToken = null;
            } else {
                status = STATUS_EXCHANGING;
            }
        } catch (final com.microsoft.tfs.jni.AuthenticationEngine.AuthenticationException e) {
            LOG.error("NTLM failure: " + e.getMessage());

            status = STATUS_ERROR;
            return null;
        }

        if (token == null || token.length == 0) {
            status = STATUS_ERROR;
            throw new AuthenticationException("NTLM Scheme did not provided token");
        }

        final String tokenBase64 = EncodingUtil.getAsciiString(Base64.encodeBase64(token));

        return "NTLM " + tokenBase64;
    }
}
