// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.httpclient;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

    /*
     * Clones the original {@link CookieCredentials} but replaces the included
     * cookies' domain with the specified value.
     */
    public CookieCredentials setDomain(final String domain) {
        final List<Cookie> newCookies = new ArrayList<Cookie>(cookies.length);

        for (final Cookie cookie : cookies) {
            final Cookie newCookie =
                new Cookie(domain, cookie.getName(), cookie.getValue(), "/", null, cookie.getSecure());
            /*
             * Setting the following property to true makes cookies added to the
             * HTTP headers contain the attribute $Path=/ and thus a semicolon
             * between the cookie value and this attribute.
             *
             * This is a workaround for a bug in cookie processing on the server
             * side: the cookie values has to be appended with a semicolon
             * otherwise an error is reported either by .NET or TFS (not clear
             * yet by which one exactly):
             *
             * "The input is not a valid Base-64 string as it contains a
             * non-base 64 character, more than two padding characters, or an
             * illegal character among the padding characters."
             */
            newCookie.setPathAttributeSpecified(true);

            newCookies.add(newCookie);
        }

        return new CookieCredentials(newCookies.toArray(new Cookie[cookies.length]));
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
