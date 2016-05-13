/*
 * $Header:
 * /home/jerenkrantz/tmp/commons/commons-convert/cvs/home/cvs/jakarta-commons
 * //httpclient/src/java/org/apache/commons/httpclient/auth/AuthState.java,v 1.3
 * 2004/11/02 19:39:16 olegk Exp $ $Revision: 480424 $ $Date: 2006-11-29
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.core.httpclient.Credentials;

/**
 * This class provides detailed information about the state of the
 * authentication process.
 *
 * @author <a href="mailto:oleg@ural.ru">Oleg Kalnichevski</a>
 * @since 3.0
 */
public class AuthState {
    public static final Class[] preemptiveAuthSchemes = new Class[] {
        CookieAuthScheme.class,
        JwtAuthScheme.class,
        PreemptiveBasicScheme.class
    };

    /** Actual authentication scheme */
    private AuthScheme authScheme = null;

    /** Whether an authentication challenged has been received */
    private boolean authRequested = false;

    /** Whether the authentication challenge has been responded to */
    private boolean authAttempted = false;

    /** Whether preemptive authentication is attempted */
    private boolean preemptive = false;

    /** Log object. */
    private static final Log LOG = LogFactory.getLog(AuthState.class);

    /**
     * Default constructor.
     *
     */
    public AuthState() {
        super();
    }

    /**
     * Invalidates the authentication state by resetting its parameters.
     */
    public void invalidate() {
        authScheme = null;
        authRequested = false;
        authAttempted = false;
        preemptive = false;
    }

    /**
     * Tests whether authentication challenge has been received
     *
     * @return <tt>true</tt> if authentication challenge has been received,
     *         <tt>false</tt> otherwise
     */
    public boolean isAuthRequested() {
        return authRequested;
    }

    /**
     * Sets authentication request status
     *
     * @param challengeReceived
     *        <tt>true</tt> if authentication has been requested, <tt>false</tt>
     *        otherwise
     */
    public void setAuthRequested(final boolean challengeReceived) {
        authRequested = challengeReceived;
    }

    /**
     * Tests whether authentication challenge has been responded to
     *
     * @return <tt>true</tt> if authentication challenge has been responded to,
     *         <tt>false</tt> otherwise
     */
    public boolean isAuthAttempted() {
        return authAttempted;
    }

    /**
     * Sets authentication attempt status
     *
     * @param challengeResponded
     *        <tt>true</tt> if authentication has been attempted, <tt>false</tt>
     *        otherwise
     */
    public void setAuthAttempted(final boolean challengeResponded) {
        authAttempted = challengeResponded;
    }

    /**
     * Preemptively assigns a suitable authentication scheme.
     */
    public void setPreemptive(final Credentials credentials) {
        if (!preemptive) {
            if (authScheme != null) {
                throw new IllegalStateException("Authentication state already initialized");
            }

            /*
             * Look at the auth schemes that are preemptive, find the first that
             * is valid for our credentials.
             *
             * See {@link AuthPolicy} for auth scheme instantiation example. We
             * do NOT use the {@link AuthPolicy} framework here, as that is used
             * by RFC-compliant (Authorization header based) authenticators
             * only.
             */
            for (int i = 0; i < preemptiveAuthSchemes.length; i++) {
                try {
                    final AuthScheme testScheme = (AuthScheme) preemptiveAuthSchemes[i].newInstance();

                    if (testScheme.supportsCredentials(credentials)) {
                        LOG.debug(
                            "Setting preemptive credentials for " + testScheme.getSchemeName() + " authentication");

                        authScheme = testScheme;
                        preemptive = true;

                        return;
                    }
                } catch (final Exception e) {
                    LOG.error("Error initializing authentication scheme: " + preemptiveAuthSchemes[i].getName(), e);
                    throw new IllegalStateException(
                        "Authentication scheme implemented by "
                            + preemptiveAuthSchemes[i].getName()
                            + " could not be initialized");
                }
            }

            LOG.info("No authentication schemes are suitable for preemptive authentication");
        }
    }

    /**
     * Tests if preemptive authentication is used.
     *
     * @return <tt>true</tt> if using the default Basic {@link AuthScheme
     *         authentication scheme}, <tt>false</tt> otherwise.
     */
    public boolean isPreemptive() {
        return preemptive;
    }

    /**
     * Assigns the given {@link AuthScheme authentication scheme}.
     *
     * @param authScheme
     *        the {@link AuthScheme authentication scheme}
     */
    public void setAuthScheme(final AuthScheme authScheme) {
        if (authScheme == null) {
            invalidate();
            return;
        }
        if (preemptive && !(this.authScheme.getClass().isInstance(authScheme))) {
            preemptive = false;
            authAttempted = false;
        }
        this.authScheme = authScheme;
    }

    /**
     * Returns the {@link AuthScheme authentication scheme}.
     *
     * @return {@link AuthScheme authentication scheme}
     */
    public AuthScheme getAuthScheme() {
        return authScheme;
    }

    @Override
    public String toString() {
        final StringBuffer buffer = new StringBuffer();
        buffer.append("Auth state: auth requested [");
        buffer.append(authRequested);
        buffer.append("]; auth attempted [");
        buffer.append(authAttempted);
        if (authScheme != null) {
            buffer.append("]; auth scheme [");
            buffer.append(authScheme.getSchemeName());
        }
        buffer.append("] preemptive [");
        buffer.append(preemptive);
        buffer.append("]");
        return buffer.toString();
    }
}
