// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.httpclient;

import java.util.Arrays;

/**
 * Credentials that are sent as Cookie headers.
 *
 * @threadsafety unknown
 */
public class CookieCredentials extends Credentials {
    private final Cookie[] cookies;

    /**
     * Creates {@link CookieCredentials} from the given cookies.
     *
     * @param cookies
     *        The {@link Cookie}s to send with requests. (not <code>null</code>)
     */
    public CookieCredentials(final Cookie[] cookies) {
        if (cookies == null) {
            throw new IllegalArgumentException("Cookies may not be null");
        }

        this.cookies = cookies;
    }

    public Cookie[] getCookies() {
        return cookies;
    }

    @Override
    public String toString() {
        return "(Cookie Credentials)";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode(cookies);
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final CookieCredentials other = (CookieCredentials) obj;
        if (!Arrays.equals(cookies, other.cookies)) {
            return false;
        }
        return true;
    }
}
