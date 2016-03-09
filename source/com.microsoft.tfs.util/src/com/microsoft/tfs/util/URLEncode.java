// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.util;

import java.net.URI;
import java.net.URISyntaxException;

public class URLEncode {
    public static String encode(final String input) {
        /*
         * -- java.net.URLEncoder is not the right class to use. It does
         * application/x-www-form-urlencoded encoding, not RFC 2396 URL
         * encoding. See:
         * http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4616184
         * http://www.
         * velocityreviews.com/forums/t145114-force-urlencoder-to-use-
         * 20-instead-of-for-spaces.html
         *
         * -- java.net.URI appears to implement the RFC 2396 style encoding we
         * want. However, the only way I've found to get it to encode an input
         * string is to use one of the multi-value constructors. If there is a
         * better way, feel free to change this code.
         */
        try {
            return new URI(null, null, input, null).toASCIIString();
        } catch (final URISyntaxException ex) {
            throw new RuntimeException(input, ex);
        }
    }
}
