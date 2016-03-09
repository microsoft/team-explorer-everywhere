/*
 * $Header:
 * /home/jerenkrantz/tmp/commons/commons-convert/cvs/home/cvs/jakarta-commons
 * //httpclient/src/java/org/apache/commons/httpclient/auth/BasicScheme.java,v
 * 1.17 2004/05/13 04:02:00 mbecke Exp $ $Revision: 480424 $ $Date: 2006-11-29
 * 06:56:49 +0100 (Wed, 29 Nov 2006) $
 *
 * ====================================================================
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many individuals on
 * behalf of the Apache Software Foundation. For more information on the Apache
 * Software Foundation, please see <http://www.apache.org/>.
 */

package com.microsoft.tfs.core.httpclient.auth;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.core.httpclient.Credentials;
import com.microsoft.tfs.core.httpclient.HttpMethod;
import com.microsoft.tfs.core.httpclient.URIException;
import com.microsoft.tfs.core.httpclient.UsernamePasswordCredentials;
import com.microsoft.tfs.core.httpclient.util.EncodingUtil;

/**
 * <p>
 * Basic authentication scheme as defined in RFC 2617.
 * </p>
 *
 * @author <a href="mailto:remm@apache.org">Remy Maucherat</a>
 * @author Rodney Waldhoff
 * @author <a href="mailto:jsdever@apache.org">Jeff Dever</a>
 * @author Ortwin Gl?ck
 * @author Sean C. Sullivan
 * @author <a href="mailto:adrian@ephox.com">Adrian Sutton</a>
 * @author <a href="mailto:mbowler@GargoyleSoftware.com">Mike Bowler</a>
 * @author <a href="mailto:oleg@ural.ru">Oleg Kalnichevski</a>
 */

public class BasicScheme extends RFC2617Scheme {
    private static final String INSECURE_BASIC_PROPERTY = "com.microsoft.tfs.client.allowInsecureBasic";

    /** Log object for this class. */
    private static final Log LOG = LogFactory.getLog(BasicScheme.class);

    /** Whether the basic authentication process is complete */
    private boolean complete;

    /**
     * Default constructor for the basic authetication scheme.
     *
     * @since 3.0
     */
    public BasicScheme() {
        super();
        complete = false;
    }

    /**
     * Constructor for the basic authetication scheme.
     *
     * @param challenge
     *        authentication challenge
     *
     * @throws MalformedChallengeException
     *         is thrown if the authentication challenge is malformed
     *
     * @deprecated Use parameterless constructor and
     *             {@link AuthScheme#processChallenge(String)} method
     */
    @Deprecated
    public BasicScheme(final String challenge) throws MalformedChallengeException {
        super(challenge);
        complete = true;
    }

    @Override
    public boolean supportsCredentials(final Credentials credentials) {
        if (credentials == null) {
            return false;
        }

        return (credentials instanceof UsernamePasswordCredentials);
    }

    /**
     * Returns textual designation of the basic authentication scheme.
     *
     * @return <code>basic</code>
     */
    @Override
    public String getSchemeName() {
        return "basic";
    }

    /**
     * Processes the Basic challenge.
     *
     * @param challenge
     *        the challenge string
     *
     * @throws MalformedChallengeException
     *         is thrown if the authentication challenge is malformed
     *
     * @since 3.0
     */
    @Override
    public void processChallenge(final String challenge) throws MalformedChallengeException {
        // super.processChallenge(challenge);
        complete = true;
    }

    /**
     * Tests if the Basic authentication process has been completed.
     *
     * @return <tt>true</tt> if Basic authorization has been processed,
     *         <tt>false</tt> otherwise.
     *
     * @since 3.0
     */
    @Override
    public boolean isComplete() {
        return complete;
    }

    /**
     * Returns <tt>false</tt>. Basic authentication scheme is request based.
     *
     * @return <tt>false</tt>.
     *
     * @since 3.0
     */
    @Override
    public boolean isConnectionBased() {
        return false;
    }

    /**
     * Produces basic authorization string for the given set of
     * {@link Credentials}.
     *
     * @param credentials
     *        The set of credentials to be used for athentication
     * @param method
     *        The method being authenticated
     * @throws InvalidCredentialsException
     *         if authentication credentials are not valid or not applicable for
     *         this authentication scheme
     * @throws AuthenticationException
     *         if authorization string cannot be generated due to an
     *         authentication failure
     *
     * @return a basic authorization string
     *
     * @since 3.0
     */
    @Override
    public String authenticate(final AuthScope authscope, final Credentials credentials, final HttpMethod method)
        throws AuthenticationException {
        LOG.trace("enter BasicScheme.authenticate(Credentials, HttpMethod)");

        if (method == null) {
            throw new IllegalArgumentException("Method may not be null");
        }

        UsernamePasswordCredentials usernamepassword = null;

        try {
            usernamepassword = (UsernamePasswordCredentials) credentials;
        } catch (final ClassCastException e) {
            throw new InvalidCredentialsException(
                "Credentials cannot be used for basic authentication: " + credentials.getClass().getName());
        }

        try {
            if (!"https".equalsIgnoreCase(method.getURI().getScheme())
                && !"true".equalsIgnoreCase(System.getProperty(INSECURE_BASIC_PROPERTY))) {
                throw new AuthenticationSecurityException(
                    "Basic credentials are only supported over HTTPS secured connections.");
            }
        } catch (final URIException e) {
            throw new AuthenticationException("Invalid URI in method", e); //$NON-NLS-1$
        }

        return BasicScheme.authenticate(usernamepassword, method.getParams().getCredentialCharset());
    }

    /**
     * @deprecated Use
     *             {@link #authenticate(UsernamePasswordCredentials, String)}
     *
     *             Returns a basic <tt>Authorization</tt> header value for the
     *             given {@link UsernamePasswordCredentials}.
     *
     * @param credentials
     *        The credentials to encode.
     *
     * @return a basic authorization string
     */
    @Deprecated
    public static String authenticate(final UsernamePasswordCredentials credentials) {
        return authenticate(credentials, "ISO-8859-1");
    }

    /**
     * Returns a basic <tt>Authorization</tt> header value for the given
     * {@link UsernamePasswordCredentials} and charset.
     *
     * @param credentials
     *        The credentials to encode.
     * @param charset
     *        The charset to use for encoding the credentials
     *
     * @return a basic authorization string
     *
     * @since 3.0
     */
    public static String authenticate(final UsernamePasswordCredentials credentials, final String charset) {

        LOG.trace("enter BasicScheme.authenticate(UsernamePasswordCredentials, String)");

        if (credentials == null) {
            throw new IllegalArgumentException("Credentials may not be null");
        }
        if (charset == null || charset.length() == 0) {
            throw new IllegalArgumentException("charset may not be null or empty");
        }
        final StringBuffer buffer = new StringBuffer();

        buffer.append(credentials.getUsername());
        buffer.append(":");
        buffer.append(credentials.getPassword());

        return "Basic "
            + EncodingUtil.getAsciiString(Base64.encodeBase64(EncodingUtil.getBytes(buffer.toString(), charset)));
    }

}
