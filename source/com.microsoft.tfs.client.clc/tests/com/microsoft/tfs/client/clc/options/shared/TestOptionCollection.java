// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.clc.options.shared;

import com.microsoft.tfs.client.clc.exceptions.InvalidOptionValueException;
import com.microsoft.tfs.client.clc.options.URIValueOption;

import junit.framework.TestCase;

/**
 *         Tests a {@link URIValueOption} class, but since that class is
 *         abstract, actually uses a few derived classes.
 */
public class TestOptionCollection extends TestCase {
    public void testOptionCollection() throws InvalidOptionValueException {
        OptionCollection o;

        // Verify that this kind of single value option doesn't expect
        // any specific strings.
        o = new OptionCollection();
        assertNull(o.getValidOptionValues());

        // These should fail to parse.
        final String[] shouldFail = new String[] {
            null,
            "/", //$NON-NLS-1$
            "c:\\", //$NON-NLS-1$
            "nonsenseprotocol://", //$NON-NLS-1$
            "http", //$NON-NLS-1$
            "simplehostname", //$NON-NLS-1$
            "host.domain.whatever", //$NON-NLS-1$
            "host.com", //$NON-NLS-1$
            "server.com:8080", //$NON-NLS-1$
            "server.com:" //$NON-NLS-1$
        };

        for (int i = 0; i < shouldFail.length; i++) {
            final String url = shouldFail[i];
            try {
                o = new OptionServer();
                o.parseValues(url);
                fail("Should fail to parse but was successful: " + url); //$NON-NLS-1$
            } catch (final InvalidOptionValueException e) {
            }
        }

        // Should parse.

        o = new OptionServer();

        o.parseValues("http://server"); //$NON-NLS-1$
        assertEquals("http", o.getURI().getScheme()); //$NON-NLS-1$
        assertEquals("server", o.getURI().getHost()); //$NON-NLS-1$
        assertEquals("", o.getURI().getPath()); //$NON-NLS-1$

        o.parseValues("http:\\\\server"); // Yes, developers //$NON-NLS-1$
                                          // really do this.
        assertEquals("http", o.getURI().getScheme()); //$NON-NLS-1$
        assertEquals("server", o.getURI().getHost()); //$NON-NLS-1$
        assertEquals("", o.getURI().getPath()); //$NON-NLS-1$

        o.parseValues("http://server/"); //$NON-NLS-1$
        assertEquals("http", o.getURI().getScheme()); //$NON-NLS-1$
        assertEquals("server", o.getURI().getHost()); //$NON-NLS-1$
        assertEquals("/", o.getURI().getPath()); //$NON-NLS-1$

        o.parseValues("https://server"); //$NON-NLS-1$
        assertEquals("https", o.getURI().getScheme()); //$NON-NLS-1$
        assertEquals("server", o.getURI().getHost()); //$NON-NLS-1$
        assertEquals("", o.getURI().getPath()); //$NON-NLS-1$

        o.parseValues("http://server:"); //$NON-NLS-1$
        assertEquals("http", o.getURI().getScheme()); //$NON-NLS-1$
        assertEquals("server", o.getURI().getHost()); //$NON-NLS-1$
        assertEquals(-1, o.getURI().getPort());
        assertEquals("", o.getURI().getPath()); //$NON-NLS-1$

        o.parseValues("http://server:/"); //$NON-NLS-1$
        assertEquals("http", o.getURI().getScheme()); //$NON-NLS-1$
        assertEquals("server", o.getURI().getHost()); //$NON-NLS-1$
        assertEquals(-1, o.getURI().getPort());
        assertEquals("/", o.getURI().getPath()); //$NON-NLS-1$

        o.parseValues("http://server:1"); //$NON-NLS-1$
        assertEquals("http", o.getURI().getScheme()); //$NON-NLS-1$
        assertEquals("server", o.getURI().getHost()); //$NON-NLS-1$
        assertEquals(1, o.getURI().getPort());
        assertEquals("", o.getURI().getPath()); //$NON-NLS-1$

        o.parseValues("http://server:1/"); //$NON-NLS-1$
        assertEquals("http", o.getURI().getScheme()); //$NON-NLS-1$
        assertEquals("server", o.getURI().getHost()); //$NON-NLS-1$
        assertEquals(1, o.getURI().getPort());
        assertEquals("/", o.getURI().getPath()); //$NON-NLS-1$

        o.parseValues("http://server:8080"); //$NON-NLS-1$
        assertEquals("http", o.getURI().getScheme()); //$NON-NLS-1$
        assertEquals("server", o.getURI().getHost()); //$NON-NLS-1$
        assertEquals(8080, o.getURI().getPort());
        assertEquals("", o.getURI().getPath()); //$NON-NLS-1$

        o.parseValues("http://server:8080/prefix"); //$NON-NLS-1$
        assertEquals("http", o.getURI().getScheme()); //$NON-NLS-1$
        assertEquals("server", o.getURI().getHost()); //$NON-NLS-1$
        assertEquals(8080, o.getURI().getPort());
        assertEquals("/prefix", o.getURI().getPath()); //$NON-NLS-1$

        o.parseValues("http://server.multiple.domains:8080/"); //$NON-NLS-1$
        assertEquals("http", o.getURI().getScheme()); //$NON-NLS-1$
        assertEquals("server.multiple.domains", o.getURI().getHost()); //$NON-NLS-1$
        assertEquals(8080, o.getURI().getPort());
        assertEquals("/", o.getURI().getPath()); //$NON-NLS-1$

        o.parseValues("http://server.multiple.domains:443/"); //$NON-NLS-1$
        assertEquals("http", o.getURI().getScheme()); //$NON-NLS-1$
        assertEquals("server.multiple.domains", o.getURI().getHost()); //$NON-NLS-1$
        assertEquals(443, o.getURI().getPort());
        assertEquals("/", o.getURI().getPath()); //$NON-NLS-1$

        o.parseValues("http://server.multiple.domains:8080/tfs/SomeCollection"); //$NON-NLS-1$
        assertEquals("http", o.getURI().getScheme()); //$NON-NLS-1$
        assertEquals("server.multiple.domains", o.getURI().getHost()); //$NON-NLS-1$
        assertEquals(8080, o.getURI().getPort());
        assertEquals("/tfs/SomeCollection", o.getURI().getPath()); //$NON-NLS-1$
    }
}
