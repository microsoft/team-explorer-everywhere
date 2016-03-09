// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.util;

import java.net.URI;
import java.util.Comparator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <p>
 * Compares URIs for TFS servers. The comparison is a case-insensitive string
 * match on partially normalized URI strings for consistency with TFS connection
 * behavior in other clients (Visual Studio).
 * </p>
 * <p>
 * The following normalization actions are done (in the specified order) to each
 * URI before comparison:
 * </p>
 * <p>
 * <ol>
 * <li>if the path part is <code>null</code> or empty, the path part is treated
 * as "/". "http://server:8080" turns into "http://server:8080/"</li>
 * <li>if the path part is longer than one character and ends in a slash, the
 * trailing slash is removed. "http://server:8080/tfs/" turns into
 * "http://server:8080/tfs"</li>
 * </ol>
 * </p>
 */
public class ServerURIComparator implements Comparator<URI> {
    private final static Log log = LogFactory.getLog(ServerURIComparator.class);

    public static final ServerURIComparator INSTANCE = new ServerURIComparator();

    /**
     * {@inheritDoc}
     */
    @Override
    public int compare(final URI uri1, final URI uri2) {
        if (uri1 == uri2) {
            return 0;
        }

        if (uri1 == null) {
            return -1;
        }

        if (uri2 == null) {
            return 1;
        }

        /*
         * URI.compareTo(URI) compares path parts case-sensitive, and
         * lower-casing (or upper-casing) the paths during normalization can't
         * be done in a locale-invariant way, so we have to stringify the URIs
         * and use a locale-invariant comparison
         * (String.compareToIgnoreCase(String)). It's OK to ignore case in the
         * entire URI string for TFS URIs.
         */
        return ServerURIUtils.normalizeURI(uri1).toString().compareToIgnoreCase(
            ServerURIUtils.normalizeURI(uri2).toString());
    }
}
