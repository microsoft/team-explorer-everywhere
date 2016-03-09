// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.jni;

/**
 * Enumeration for the protocol field in Keychain data.
 *
 * @threadsafety unknown
 */
public class KeychainProtocol extends KeychainEnum {
    public static final KeychainProtocol HTTP = new KeychainProtocol(computeValue("http")); //$NON-NLS-1$
    public static final KeychainProtocol HTTPS = new KeychainProtocol(computeValue("htps")); //$NON-NLS-1$
    public static final KeychainProtocol HTTP_PROXY = new KeychainProtocol(computeValue("htpx")); //$NON-NLS-1$
    public static final KeychainProtocol HTTPS_PROXY = new KeychainProtocol(computeValue("htsx")); //$NON-NLS-1$
    public static final KeychainProtocol CIFS = new KeychainProtocol(computeValue("cifs")); //$NON-NLS-1$
    public static final KeychainProtocol SMB = new KeychainProtocol(computeValue("smb ")); //$NON-NLS-1$

    public static final KeychainProtocol ANY = new KeychainProtocol(0);

    private KeychainProtocol(final int value) {
        super(value);
    }
}
