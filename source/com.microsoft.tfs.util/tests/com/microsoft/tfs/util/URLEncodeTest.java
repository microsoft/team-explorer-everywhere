// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.util;

import junit.framework.TestCase;

public class URLEncodeTest extends TestCase {
    public void testPlainOldAscii() {
        assertEquals(
            "ABCDEFGHIJKLYMNOPQRSTUVWYXabcdefghijklmnopqrstuvwxyz0123456789", //$NON-NLS-1$
            URLEncode.encode("ABCDEFGHIJKLYMNOPQRSTUVWYXabcdefghijklmnopqrstuvwxyz0123456789")); //$NON-NLS-1$
    }

    public void testAsciiWithPeriods() {
        assertEquals(".net", URLEncode.encode(".net")); //$NON-NLS-1$ //$NON-NLS-2$
    }

    public void testEncodeWithSpace() {
        assertEquals("Test%20Encoding", URLEncode.encode("Test Encoding")); //$NON-NLS-1$ //$NON-NLS-2$
    }
}