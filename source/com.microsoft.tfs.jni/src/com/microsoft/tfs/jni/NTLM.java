// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.jni;

import com.microsoft.tfs.jni.internal.ntlm.NTLMException;

public interface NTLM {
    /**
     * Queries the underlying NTLM provider to determine whether "default
     * credentials" -- that is, authentication with the credentials of the
     * currently logged-in user is supported or not.
     *
     * @return true if setCredentialsDefault() is supported, false otherwise
     */
    public boolean supportsCredentialsDefault();

    /**
     * Queries the underlying NTLM provider to determine whether specified
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
     * Initializes the NTLM Engine. Note that you must call dispose() on this
     * NTLMState.
     *
     * @return An NTLMState suitable for working with.
     * @throws NTLMException
     *         if the credentials could not be initialized
     */
    public NTLMState initialize() throws NTLMException;

    /**
     * Informs the NTLM engine that we would like to use default credentials to
     * authenticate. (Ie, the credentials of the currently logged in user, or
     * the default Kerberos ticket.)
     *
     * @param state
     *        An NTLMState
     * @throws NTLMException
     *         if an error occurred configuring default credentials
     */
    public void setCredentialsDefault(NTLMState state) throws NTLMException;

    /**
     * Informs the NTLM engine that we would like to use specified credentials
     * to authenticate. (Ie, the given username, domain and password.)
     *
     * @param state
     *        An NTLMState
     * @param username
     *        The username to authenticate with
     * @param domain
     *        The domainname to authenticate with
     * @param password
     *        The password to authenticate with
     * @throws NTLMException
     *         if an error occurred configuring the credentials
     */
    public void setCredentialsSpecified(NTLMState state, String username, String domain, String password)
        throws NTLMException;

    /**
     * Configures the NTLM engine to connect to the specified target (in GSSAPI
     * syntax.) Note: NTLM authentication ignores this.
     *
     * @param state
     *        An NTLMState
     * @param target
     *        The target to authenticate with (eg, "http@HOSTNAME")
     * @throws NTLMException
     *         if an error occurred configuring the target
     */
    public void setTarget(NTLMState state, String target) throws NTLMException;

    /**
     * Configures the NTLM engine with the local hostname (note, actually unused
     * in the negotiate protocol.)
     *
     * @param state
     *        An NTLMState
     * @param localhost
     *        The localhost name to provide to the remote server
     * @throws NTLMException
     *         if an error occured configuring the local hostname.
     */
    public void setLocalhost(NTLMState state, String localhost) throws NTLMException;

    /**
     * Gets the next token to provide to the remote server
     *
     * @param state
     *        An NTLMState
     * @param inputToken
     *        A token provided from the server (if any)
     * @return The next token to provide to the server
     * @throws NTLMException
     *         if an error occurred generating the token
     */
    public byte[] getToken(NTLMState state, byte[] inputToken) throws NTLMException;

    /**
     * Queries whether the NTLM engine has completed (successfully or failed) or
     * not
     *
     * @param state
     *        An NTLMState
     * @return True if the negotiate engine has completed authentication
     *         (whether successfully or failed)
     * @throws NTLMException
     *         if an error occurred querying completion state
     */
    public boolean isComplete(NTLMState state) throws NTLMException;

    /**
     * Queries for the string representation of the most recent error to occur.
     *
     * @param state
     *        An NTLMState
     * @return An error message or null
     */
    public String getErrorMessage(NTLMState state);

    /**
     * Disposes the active NTLM state
     *
     * @param state
     *        An NTLMState
     * @throws NTLMException
     *         if an error occurred disposing the state
     */
    public void dispose(NTLMState state) throws NTLMException;

    /**
     * A generic NTLMState which native or java implementations can provide
     */
    public abstract class NTLMState {
    }
}
