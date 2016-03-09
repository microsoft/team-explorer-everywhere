// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.httpclient.auth;

import com.microsoft.tfs.core.httpclient.Credentials;
import com.microsoft.tfs.core.httpclient.PreemptiveUsernamePasswordCredentials;

/**
 * A modification on HTTP Basic to send authentication preemptively.
 */
public class PreemptiveBasicScheme extends BasicScheme {
    @Override
    public boolean supportsCredentials(final Credentials credentials) {
        if (credentials == null) {
            return false;
        }

        return (credentials instanceof PreemptiveUsernamePasswordCredentials);
    }
}
