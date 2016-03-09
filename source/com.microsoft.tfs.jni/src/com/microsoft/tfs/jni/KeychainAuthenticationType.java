// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.jni;

/**
 * Enumeration for the authentication type field in Keychain data.
 *
 * @threadsafety unknown
 */
public class KeychainAuthenticationType extends KeychainEnum {
    public static final KeychainAuthenticationType NTLM = new KeychainAuthenticationType(computeValue("ntlm")); //$NON-NLS-1$
    public static final KeychainAuthenticationType MSN = new KeychainAuthenticationType(computeValue("msna")); //$NON-NLS-1$
    public static final KeychainAuthenticationType DPA = new KeychainAuthenticationType(computeValue("dpaa")); //$NON-NLS-1$
    public static final KeychainAuthenticationType HTTP_BASIC = new KeychainAuthenticationType(computeValue("http")); //$NON-NLS-1$
    public static final KeychainAuthenticationType HTTP_DIGEST = new KeychainAuthenticationType(computeValue("httd")); //$NON-NLS-1$
    public static final KeychainAuthenticationType HTML_FORM = new KeychainAuthenticationType(computeValue("form")); //$NON-NLS-1$
    public static final KeychainAuthenticationType DEFAULT = new KeychainAuthenticationType(computeValue("dflt")); //$NON-NLS-1$

    public static final KeychainAuthenticationType ANY = new KeychainAuthenticationType(0);

    private KeychainAuthenticationType(final int value) {
        super(value);
    }
}
