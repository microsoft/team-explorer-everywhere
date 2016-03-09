// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.httpclient;

/**
 * A modification of the standard username/password credentials that support
 * preemptive authentication.
 *
 * @threadsafety unknown
 */
public class PreemptiveUsernamePasswordCredentials extends UsernamePasswordCredentials {
    public PreemptiveUsernamePasswordCredentials(final String username, final String password) {
        super(username, password);
    }

    public static PreemptiveUsernamePasswordCredentials newFrom(final UsernamePasswordCredentials credentials) {
        return new PreemptiveUsernamePasswordCredentials(credentials.getUsername(), credentials.getPassword());
    }
}
