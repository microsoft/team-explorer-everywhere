// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.util;

import com.microsoft.tfs.core.httpclient.Credentials;
import com.microsoft.tfs.core.httpclient.DefaultNTCredentials;
import com.microsoft.tfs.core.httpclient.UsernamePasswordCredentials;
import com.microsoft.tfs.jni.NTLMEngine;
import com.microsoft.tfs.jni.NegotiateEngine;
import com.microsoft.tfs.util.Check;

/**
 * Utility methods for dealing with {@link Credentials} used for authenticating
 * to Team Foundation Servers.
 *
 * @threadsafety unknown
 */
public class CredentialsUtils {
    /* Prevent instantiation. */
    private CredentialsUtils() {
    }

    /**
     * Determines whether a set of credentials needs a password: this will be
     * true for credential types which contain a password where the password is
     * unset (<code>null</code>).
     *
     * @param credentials
     *        the credentials to check
     * @return <code>true</code> if the given {@link Credentials} requires a
     *         password, <code>false</code> otherwise
     */
    public static boolean needsPassword(final Credentials credentials) {
        Check.notNull(credentials, "credentials"); //$NON-NLS-1$

        return (credentials instanceof UsernamePasswordCredentials
            && ((UsernamePasswordCredentials) credentials).getPassword() == null);
    }

    /**
     * <p>
     * Determines whether the current environment supports
     * {@link DefaultNTCredentials} (ie, "single signon" using NTLM and/or
     * Kerberos authentication mechanisms.) This does not imply that the
     * credentials will be accepted by the server, just that we have credentials
     * that may be attempted.
     * </p>
     * <p>
     * The exact nature of whether this is supported is dependent on the
     * operating system and system libraries. Generally speaking, this will
     * always be true on Windows and will be true on Unix platforms if kerberos
     * libraries are installed AND there is a default principal.
     * </p>
     *
     * @return <code>true</code> if default credentials are supported,
     *         <code>false</code> otherwise
     */
    public static boolean supportsDefaultCredentials() {
        return (NegotiateEngine.getInstance().isAvailable()
            && NegotiateEngine.getInstance().supportsCredentialsDefault())
            || (NTLMEngine.getInstance().isAvailable() && NTLMEngine.getInstance().supportsCredentialsDefault());
    }

    /**
     * <p>
     * Determines whether the current environments supports
     * {@link NTCredentials} (username/password/domain based credentials.) This
     * does not imply that any given credentials will be accepted by the server,
     * just that we have the means to communicate them. Further, it does not
     * imply what these credentials should be.
     * </p>
     * <p>
     * This is always expected to be true.
     * </p>
     *
     * @return <code>true</code> if specified credentials are supported,
     *         <code>false</code> otherwise
     */
    public static boolean supportsSpecifiedCredentials() {
        return (NegotiateEngine.getInstance().isAvailable()
            && NegotiateEngine.getInstance().supportsCredentialsSpecified())
            || (NTLMEngine.getInstance().isAvailable() && NTLMEngine.getInstance().supportsCredentialsSpecified());
    }
}
