// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.jni;

/**
 * This is the interface for native / Java authentication methods. This is
 * implemented notably by SPNEGO and NTLM authentication at present, but should
 * be expendable to any challenge/response (Digest) mechanism or even simple
 * password mechanisms (Basic).
 *
 * These differ slightly from the standard JNI *Utils classes to provide a
 * slightly more rich and OO-style authentication support for clients.
 */
public interface AuthenticationEngine {
    /**
     * If the given authentication mechanism is available on this system.
     *
     * @return true if this authentication mechanism is available, false
     *         otherwise.
     */
    public boolean isAvailable();

    /**
     * Queries the authentication mechanism for whether it supports default
     * credentials (ie, passwordless or single signon.)
     *
     * @return true if this authentication mechanism supports default
     *         credentials, false otherwise
     */
    public boolean supportsCredentialsDefault();

    /**
     * Queries the authentication mechanism for whether it supports specified
     * credentials (ie, username/domain/password.)
     *
     * @return true if this authentication mechanism supports specified
     *         credentials, false otherwise
     */
    public boolean supportsCredentialsSpecified();

    /**
     * Gets the default credentials (username and domain only) if they are
     * supported by this authentication mechanism.
     *
     * @return Credentials in the form user@DOMAIN if they are supported, or
     *         null if they are not supported
     */
    public String getCredentialsDefault();

    /**
     * Gets a new authentication client which can be used to authenticate to a
     * remote server. Clients must call dispose() on the created
     * AuthenticationClient.
     *
     * @return A new AuthenticationClient which can be used for a single
     *         authentication session.
     * @throws AuthenticationException
     *         If the authentication mechanism could not be instantiated
     */
    public AuthenticationClient newClient() throws AuthenticationException;

    /**
     * An interface which provides a single authentication session with a
     * particular authentication protocol.
     */
    public interface AuthenticationClient {
        /**
         * Informs the authentication client that we would like to use default
         * credentials to authenticate. (Ie, the credentials of the currently
         * logged in user, or the default Kerberos ticket.)
         *
         * @throws AuthenticationException
         *         if an error occured configuring default credentials
         */
        public void setCredentialsDefault() throws AuthenticationException;

        /**
         * Informs the authentication client that we would like to use the
         * specified username, domain and password to authenticate.
         *
         * @param username
         *        The username to authenticate with
         * @param domain
         *        The domain to authenticate with
         * @param password
         *        The password to authenticate with
         * @throws AuthenticationException
         *         If the specified credentials could not be configured
         */
        public void setCredentialsSpecified(String username, String domain, String password)
            throws AuthenticationException;

        /**
         * Sets the target (remote host) for authentication.
         *
         * @param target
         *        The authentication target in GSSAPI format
         *        ('protocol@HOSTNAME').
         * @throws AuthenticationException
         *         If the target could not be configured.
         */
        public void setTarget(String target) throws AuthenticationException;

        /**
         * Gets the next "token" to send to the remote server for
         * authentication.
         *
         * @param inputToken
         *        The "token" delivered by the server, or null if no token was
         *        sent
         * @return The next token to deliver to the server
         * @throws AuthenticationException
         *         If the next token could not be retrieved
         */
        public byte[] getToken(byte[] inputToken) throws AuthenticationException;

        /**
         * Determines whether authentication is complete.
         *
         * @return true if authentication has completed (in success or failure),
         *         false if we need to continue sending / expecting tokens
         * @throws AuthenticationException
         *         If an error occurred
         */
        public boolean isComplete() throws AuthenticationException;

        /**
         * Returns the most recent error to occur, or null if none have
         * occurred.
         *
         * @return An error message or null
         */
        public String getErrorMessage();

        /**
         * Disposes any resources associated with this AuthenticationClient.
         */
        public void dispose();
    }

    /**
     * A generic exception for authentication.
     */
    public abstract class AuthenticationException extends Exception {
        /**
         * Auto-generated.
         */
        private static final long serialVersionUID = 1549334140199949349L;

        public AuthenticationException(final String message) {
            super(message);
        }

        public AuthenticationException(final String message, final Throwable t) {
            super(message, t);
        }
    }
}
