// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.httpclient.cookie;

/**
 * Cookie spec class that aims for maximum compatibility with web practices, if
 * not necessarily standards.
 *
 * @threadsafety unknown
 */
public class CompatibilityCookieSpec extends RFC2109Spec {
    public CompatibilityCookieSpec() {
        super();
    }

    /**
     * Performs domain-match as implemented by browsers.
     *
     * @param host
     *        The target host.
     * @param domain
     *        The cookie domain attribute.
     * @return true if the specified host matches the given domain.
     *
     * @since 3.0
     */
    @Override
    public boolean domainMatch(final String host, final String domain) {
        if (host.equals(domain)) {
            return true;
        }

        /*
         * Domains match with all browsers when the hostname equals the domain
         * with a leading dot. (ie, a cookie with domain ".microsoft.com"
         * matches for host "microsoft.com"
         */
        if (domain.equals("." + host)) {
            return true;
        }

        if (domain.startsWith(".") && host.endsWith(domain)) {
            return true;
        }

        return false;
    }
}
