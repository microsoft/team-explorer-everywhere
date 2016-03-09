// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.httpclient.auth;

import java.security.Provider;

public class NTLMSecurityProvider extends Provider {
    public NTLMSecurityProvider() {
        super("NTLMSecurity", 1.0, "NTLM Security Provider");

        put("MessageDigest.MD4", "cryptix.jce.provider.md.MD4");
    }
}
