// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.util;

import java.net.URI;

import junit.framework.TestCase;

public class ServerURIUtilsTest extends TestCase {
    public void testNormalizeURI() {
        URI uri;

        uri = ServerURIUtils.normalizeURI(URIUtils.newURI("http://abc/def?asdf")); //$NON-NLS-1$
        assertEquals("http://abc/def", uri.toString()); //$NON-NLS-1$

        uri = ServerURIUtils.normalizeURI(URIUtils.newURI("HTTP://abc/def")); //$NON-NLS-1$
        assertEquals("http://abc/def", uri.toString()); //$NON-NLS-1$

        uri = ServerURIUtils.normalizeURI(URIUtils.newURI("http://abc")); //$NON-NLS-1$
        assertEquals("http://abc/", uri.toString()); //$NON-NLS-1$

        uri = ServerURIUtils.normalizeURI(URIUtils.newURI("http://abc/def/////")); //$NON-NLS-1$
        assertEquals("http://abc/def", uri.toString()); //$NON-NLS-1$
    }
}
