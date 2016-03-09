// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.util;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

import com.microsoft.tfs.util.LocaleInvariantStringHelpers;

public class ServerURIUtils {
    /**
     * If the entered host name ends with this domain name, the scheme is
     * automatically chosen.
     */
    public static final String[] HOSTED_SERVICE_DOMAIN_SUFFIXES = {
        ".tfsallin.net", //$NON-NLS-1$
        ".visualstudio.com" //$NON-NLS-1$
    };
    public static final String HOSTED_SERVICE_DEFAULT_SCHEME = "https"; //$NON-NLS-1$

    private ServerURIUtils() {
    }

    /**
     * Canonicalizes the URI into "TFS format". This function preserves the
     * given case on the URI, and is not suitable for serialization or sharing
     * with Visual Studio (which lowercases URIs.)
     *
     * @param uri
     *        the server URI
     * @return the server URI, canonicalized
     */
    public static URI normalizeURI(final URI uri) {
        return normalizeURI(uri, false);
    }

    /**
     * Canonicalizes the URI into TFS format.
     *
     * @param uri
     *        the server URI
     * @param lowercase
     *        <code>true</code> to flatten the URI into lowercase,
     *        <code>false</code> to preserve case
     * @return the server URI, canonicalized
     */
    public static URI normalizeURI(final URI uri, final boolean lowercase) {
        if (uri == null) {
            return null;
        }

        // Convert null or empty path to "/"
        String pathPart = (uri.getPath() == null || uri.getPath().length() == 0) ? "/" : uri.getPath(); //$NON-NLS-1$

        // Remove trailing slash if there is more than one character
        while (pathPart.length() > 1 && pathPart.endsWith("/")) //$NON-NLS-1$
        {
            pathPart = pathPart.substring(0, pathPart.length() - 1);
        }

        /* Always lowercase scheme for sanity. */
        final String scheme = (uri.getScheme() == null) ? null : uri.getScheme().toLowerCase(Locale.ENGLISH);
        String host = uri.getHost();

        if (lowercase) {
            pathPart = pathPart.toLowerCase(Locale.ENGLISH);
            host = (host == null) ? null : host.toLowerCase(Locale.ENGLISH);
        }

        try {
            // Use null query and fragment as these are not important for TFS
            // URIs.
            return new URI(scheme, uri.getUserInfo(), host, uri.getPort(), pathPart, null, null);
        } catch (final URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * <p>
     * Compares URIs for TFS servers according to {@link ServerURIComparator}.
     * </p>
     */
    public static boolean equals(final URI one, final URI two) {
        return (ServerURIComparator.INSTANCE.compare(one, two) == 0);
    }

    public static boolean isHosted(final URI uri) {
        return isHosted(uri.getHost());
    }

    public static boolean isHosted(final String uriServerName) {
        for (int i = 0; i < HOSTED_SERVICE_DOMAIN_SUFFIXES.length; i++) {
            // DNS names may end with a final ".", so check for both.
            if (LocaleInvariantStringHelpers.caseInsensitiveEndsWith(uriServerName, HOSTED_SERVICE_DOMAIN_SUFFIXES[i])
                || LocaleInvariantStringHelpers.caseInsensitiveEndsWith(
                    uriServerName,
                    HOSTED_SERVICE_DOMAIN_SUFFIXES[i] + ".")) //$NON-NLS-1$
            {
                return true;
            }
        }

        return false;
    }
}
