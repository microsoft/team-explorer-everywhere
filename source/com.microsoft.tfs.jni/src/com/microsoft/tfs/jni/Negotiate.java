// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.jni;

import com.microsoft.tfs.jni.internal.negotiate.NegotiateException;

public interface Negotiate {
    /**
     * Queries the underlying Negotiate provider to determine whether "default
     * credentials" -- that is, authentication with the credentials of the
     * currently logged-in user is supported or not.
     *
     * @return true if setCredentialsDefault() is supported, false otherwise
     */
    public boolean supportsCredentialsDefault();

    /**
     * Queries the underlying Negotiate provider to determine whether specified
     * credentials - username, password, etc are available to use for
     * authentication.
     *
     * @return true if setCredentials(string, string, string) is supported,
     *         false otherwise
     */
    public boolean supportsCredentialsSpecified();

    /**
     * Gets the default credentials - if supported - for the negotiate
     * mechanism. The format is username@DOMAIN.
     *
     * @return The default credentials (username@DOMAIN) or null if they could
     *         not be loaded or are unsupported.
     */
    public String getCredentialsDefault();

    /**
     * Initializes the Negotiate Engine. Note that you must call dispose() on
     * this NegotiateState.
     *
     * @return An NegotiateState suitable for working with.
     * @throws NegotiateException
     *         if the credentials could not be initialized
     */
    public NegotiateState initialize() throws NegotiateException;

    /**
     * Informs the negotiate engine that we would like to use default
     * credentials to authenticate. (Ie, the credentials of the currently logged
     * in user, or the default Kerberos ticket.)
     *
     * @param state
     *        A NegotiateState
     * @throws NegotiateException
     *         if an error occurred configuring default credentials
     */
    public void setCredentialsDefault(NegotiateState state) throws NegotiateException;

    /**
     * Informs the negotiate engine that we would like to use specified
     * credentials to authenticate. (Ie, the given username, domain and
     * password.)
     *
     * @param state
     *        A NegotiateState
     * @param username
     *        The username to authenticate with
     * @param domain
     *        The domainname to authenticate with
     * @param password
     *        The password to authenticate with
     * @throws NegotiateException
     *         if an error occurred configuring the credentials
     */
    public void setCredentialsSpecified(NegotiateState state, String username, String domain, String password)
        throws NegotiateException;

    /**
     * Configures the negotiate engine to connect to the specified target (in
     * GSSAPI syntax.)
     *
     * @param state
     *        A NegotiateState
     * @param target
     *        The target to authenticate with (eg, "http@HOSTNAME")
     * @throws NegotiateException
     *         if an error occurred configuring the target
     */
    public void setTarget(NegotiateState state, String target) throws NegotiateException;

    /**
     * Configures the negotiate engine with the local hostname (note, actually
     * unused in the negotiate protocol.)
     *
     * @param state
     *        A NegotiateState
     * @param localhost
     *        The localhost name to provide to the remote server
     * @throws NegotiateException
     *         if an error occured configuring the local hostname.
     */
    public void setLocalhost(NegotiateState state, String localhost) throws NegotiateException;

    /**
     * Gets the next token to provide to the remote server
     *
     * @param state
     *        A NegotiateState
     * @param inputToken
     *        A token provided from the server (if any)
     * @return The next token to provide to the server
     * @throws NegotiateException
     *         if an error occurred generating the token
     */
    public byte[] getToken(NegotiateState state, byte[] inputToken) throws NegotiateException;

    /**
     * Queries whether the negotiate engine has completed (successfully or
     * failed) or not
     *
     * @param state
     *        A NegotiateState
     * @return True if the negotiate engine has completed authentication
     *         (whether successfully or failed)
     * @throws NegotiateException
     *         if an error occurred querying completion state
     */
    public boolean isComplete(NegotiateState state) throws NegotiateException;

    /**
     * Queries for the string representation of the most recent error to occur.
     *
     * @param state
     *        A NegotiateState
     * @return An error message or null
     */
    public String getErrorMessage(NegotiateState state);

    /**
     * Disposes the active negotiate state
     *
     * @param state
     *        A NegotiateState
     * @throws NegotiateException
     *         if an error occurred disposing the state
     */
    public void dispose(NegotiateState state) throws NegotiateException;

    /**
     * A generic NegotiateState which native or java implementations can provide
     */
    public abstract class NegotiateState {
    }
}