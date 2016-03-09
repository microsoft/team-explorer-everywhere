// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.httpclient.auth;

public class WindowsUser {
    private final String username;
    private final String domain;

    /**
     * <p>
     * Constructs a WindowsUser from the given username (and optionally, domain)
     * string. These styles are supported:
     * </p>
     * <p>
     * <ul>
     * <li>username</li>
     * <li>username@domain</li>
     * <li>domain\\username</li>
     * </ul>
     * </p>
     * <p>
     * If a separator is found at the beginning or end of the input, the
     * username or domain will be considered empty, respectively.
     * </p>
     *
     * @param usernameAndOptionalDomain
     *        the username and optional domain string
     */
    public WindowsUser(final String usernameAndOptionalDomain) {
        if (usernameAndOptionalDomain == null) {
            username = "";
            domain = "";
            return;
        }

        final int indexOfBackslash = usernameAndOptionalDomain.indexOf('\\');
        final int indexOfAtSign = usernameAndOptionalDomain.indexOf('@');

        if (indexOfBackslash >= 0) {
            domain = usernameAndOptionalDomain.substring(0, indexOfBackslash);
            username = usernameAndOptionalDomain.substring(indexOfBackslash + 1);
        } else if (indexOfAtSign >= 0) {
            username = usernameAndOptionalDomain.substring(0, indexOfAtSign);
            domain = usernameAndOptionalDomain.substring(indexOfAtSign + 1);
        } else {
            // No separator found, so the whole string must be the username.
            username = usernameAndOptionalDomain;
            domain = ""; //$NON-NLS-1$
        }
    }

    public String getUsername() {
        return username;
    }

    public String getDomain() {
        return domain;
    }
}
