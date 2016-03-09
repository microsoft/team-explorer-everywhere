// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.clc;

import static com.microsoft.tfs.client.clc.CLCHTTPClientFactory.hostExcludedFromProxyEnvironment;
import static com.microsoft.tfs.client.clc.CLCHTTPClientFactory.hostExcludedFromProxyProperties;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.net.URI;

import org.junit.Test;

public class CLCHTTPClientFactoryTest {
    @Test
    public void testHostProxiedFromProperties() throws Exception {
        final URI uri = new URI("http://www.bar.com/"); //$NON-NLS-1$

        assertFalse(hostExcludedFromProxyProperties(uri, null));
        assertFalse(hostExcludedFromProxyProperties(uri, "")); //$NON-NLS-1$
        assertTrue(hostExcludedFromProxyProperties(uri, "*")); //$NON-NLS-1$

        /* Test some single entries */
        assertTrue(hostExcludedFromProxyProperties(uri, "www.bar.com")); //$NON-NLS-1$
        assertFalse(hostExcludedFromProxyProperties(uri, "www.server.com")); //$NON-NLS-1$

        /* Test multiple entries in the no_proxy environment variable */
        assertTrue(hostExcludedFromProxyProperties(uri, "www.bar.com|www.server.com|www.asdf.com")); //$NON-NLS-1$
        assertTrue(hostExcludedFromProxyProperties(uri, "www.server.com|www.bar.com|www.asdf.com")); //$NON-NLS-1$
        assertFalse(hostExcludedFromProxyProperties(uri, "www.server.com|www.asdf.com|www.zippy.com")); //$NON-NLS-1$
        assertFalse(hostExcludedFromProxyProperties(uri, "server.com|bar.com|asdf.com")); //$NON-NLS-1$

        /* Test wildcard prefixes */
        assertTrue(hostExcludedFromProxyProperties(uri, "*.bar.com")); //$NON-NLS-1$
        assertTrue(hostExcludedFromProxyProperties(uri, "*bar.com")); //$NON-NLS-1$
        assertTrue(hostExcludedFromProxyProperties(uri, "*www.bar.com")); //$NON-NLS-1$
        assertFalse(hostExcludedFromProxyProperties(uri, "*.server.com")); //$NON-NLS-1$
        assertFalse(hostExcludedFromProxyProperties(uri, "*server.com")); //$NON-NLS-1$

        /* Test wildcard suffixes */
        assertTrue(hostExcludedFromProxyProperties(uri, "www.*")); //$NON-NLS-1$
        assertTrue(hostExcludedFromProxyProperties(uri, "www.bar.*")); //$NON-NLS-1$
        assertFalse(hostExcludedFromProxyProperties(uri, "www.server.*")); //$NON-NLS-1$
        assertFalse(hostExcludedFromProxyProperties(uri, "192.168.24.*")); //$NON-NLS-1$

        /* Test wildcards in multiple entries */
        assertTrue(hostExcludedFromProxyProperties(uri, "*.bar.com|*.server.com|*.asdf.com")); //$NON-NLS-1$
        assertTrue(hostExcludedFromProxyProperties(uri, "*.server.com|*.bar.com|*.asdf.com")); //$NON-NLS-1$
        assertFalse(hostExcludedFromProxyProperties(uri, "*.zippy.com|*.server.com|*.asdf.com")); //$NON-NLS-1$
    }

    @Test
    public void testHostProxiedFromEnvironment() throws Exception {
        final URI uri = new URI("http://www.server.mil/"); //$NON-NLS-1$

        assertFalse(hostExcludedFromProxyEnvironment(uri, null));
        assertFalse(hostExcludedFromProxyEnvironment(uri, "")); //$NON-NLS-1$
        assertTrue(hostExcludedFromProxyEnvironment(uri, "*")); //$NON-NLS-1$

        /* Test some single entries */
        assertTrue(hostExcludedFromProxyEnvironment(uri, ".mil")); //$NON-NLS-1$
        assertTrue(hostExcludedFromProxyEnvironment(uri, ".server.mil")); //$NON-NLS-1$
        assertFalse(hostExcludedFromProxyEnvironment(uri, ".com")); //$NON-NLS-1$
        assertFalse(hostExcludedFromProxyEnvironment(uri, ".il")); //$NON-NLS-1$

        /* Test multiple entries in the no_proxy environment variable */
        assertTrue(hostExcludedFromProxyEnvironment(uri, ".mil,.com,.net")); //$NON-NLS-1$
        assertTrue(hostExcludedFromProxyEnvironment(uri, ".bar.mil,.asdf.mil,.server.mil")); //$NON-NLS-1$
        assertTrue(hostExcludedFromProxyEnvironment(uri, "mil,com,net")); //$NON-NLS-1$
        assertTrue(hostExcludedFromProxyEnvironment(uri, "com,net,org,il,arpa")); //$NON-NLS-1$
        assertTrue(hostExcludedFromProxyEnvironment(uri, "bar.mil,asdf.mil,server.mil")); //$NON-NLS-1$
        assertFalse(hostExcludedFromProxyEnvironment(uri, ".com,.net,.org,.il")); //$NON-NLS-1$
        assertFalse(hostExcludedFromProxyEnvironment(uri, "bar.il,asdf.il,server.il")); //$NON-NLS-1$

        /*
         * Test that this is just a substring match. As the documentation for
         * lynx points out:
         *
         * Warning: Note that setting 'il' as an entry in this list will block
         * proxying for the .mil domain as well as the .il domain. If the entry
         * is '.il' this will not happen.
         *
         * http://lynx.isc.org/lynx2.8.6/lynx2-8-6/lynx_help/keystrokes/
         * environments .html
         */
        assertTrue(hostExcludedFromProxyEnvironment(uri, "il")); //$NON-NLS-1$
        assertFalse(hostExcludedFromProxyEnvironment(uri, ".il")); //$NON-NLS-1$

        /* Port numbers must match */
        assertTrue(hostExcludedFromProxyEnvironment(uri, ".mil:80")); //$NON-NLS-1$
        assertTrue(hostExcludedFromProxyEnvironment(uri, ".mil:80")); //$NON-NLS-1$
        assertFalse(hostExcludedFromProxyEnvironment(uri, ".mil:8080")); //$NON-NLS-1$

        assertTrue(hostExcludedFromProxyEnvironment(new URI("http://www.server.mil/"), ".mil")); //$NON-NLS-1$ //$NON-NLS-2$
        assertTrue(hostExcludedFromProxyEnvironment(new URI("http://www.server.mil/"), ".mil:80")); //$NON-NLS-1$ //$NON-NLS-2$
        assertFalse(hostExcludedFromProxyEnvironment(new URI("http://www.server.mil/"), ".mil:443")); //$NON-NLS-1$ //$NON-NLS-2$
        assertFalse(hostExcludedFromProxyEnvironment(new URI("http://www.server.mil/"), ".mil:8080")); //$NON-NLS-1$ //$NON-NLS-2$

        assertTrue(hostExcludedFromProxyEnvironment(new URI("https://www.server.mil/"), ".mil")); //$NON-NLS-1$ //$NON-NLS-2$
        assertFalse(hostExcludedFromProxyEnvironment(new URI("https://www.server.mil/"), ".mil:80")); //$NON-NLS-1$ //$NON-NLS-2$
        assertTrue(hostExcludedFromProxyEnvironment(new URI("https://www.server.mil/"), ".mil:443")); //$NON-NLS-1$ //$NON-NLS-2$
        assertFalse(hostExcludedFromProxyEnvironment(new URI("https://www.server.mil/"), ".mil:8080")); //$NON-NLS-1$ //$NON-NLS-2$

        assertTrue(hostExcludedFromProxyEnvironment(new URI("http://www.server.mil:8080/"), ".mil")); //$NON-NLS-1$ //$NON-NLS-2$
        assertFalse(hostExcludedFromProxyEnvironment(new URI("http://www.server.mil:8080/"), ".mil:80")); //$NON-NLS-1$ //$NON-NLS-2$
        assertFalse(hostExcludedFromProxyEnvironment(new URI("http://www.server.mil:8080/"), ".mil:443")); //$NON-NLS-1$ //$NON-NLS-2$
        assertTrue(hostExcludedFromProxyEnvironment(new URI("http://www.server.mil:8080/"), ".mil:8080")); //$NON-NLS-1$ //$NON-NLS-2$

        /*
         * Wildcards are NOT accepted - only as the only entry, however they
         * should be ignored
         */
        assertFalse(hostExcludedFromProxyEnvironment(uri, "*.mil")); //$NON-NLS-1$
        assertFalse(hostExcludedFromProxyEnvironment(uri, "*mil")); //$NON-NLS-1$
        assertFalse(hostExcludedFromProxyEnvironment(uri, "*il")); //$NON-NLS-1$
        assertTrue(hostExcludedFromProxyEnvironment(uri, "*,mil")); //$NON-NLS-1$
    }
}
