// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.externaltools;

import junit.framework.TestCase;

public class WindowsStyleArgumentTokenizerTest extends TestCase {
    public void testGetRawFirstToken() {
        // Preserves only leading space
        assertEquals("fun", WindowsStyleArgumentTokenizer.getRawFirstToken("fun")); //$NON-NLS-1$ //$NON-NLS-2$
        assertEquals("fun", WindowsStyleArgumentTokenizer.getRawFirstToken("fun ")); //$NON-NLS-1$ //$NON-NLS-2$
        assertEquals(" fun", WindowsStyleArgumentTokenizer.getRawFirstToken(" fun ")); //$NON-NLS-1$ //$NON-NLS-2$
        assertEquals(" \t fun", WindowsStyleArgumentTokenizer.getRawFirstToken(" \t fun \t ")); //$NON-NLS-1$ //$NON-NLS-2$

        // Preserve quotes
        assertEquals("\"fun\"", WindowsStyleArgumentTokenizer.getRawFirstToken("\"fun\"")); //$NON-NLS-1$ //$NON-NLS-2$
        assertEquals(" \"fun\"", WindowsStyleArgumentTokenizer.getRawFirstToken(" \"fun\"")); //$NON-NLS-1$ //$NON-NLS-2$

        // Preserve quotes with spaces
        assertEquals("\"fun\"", WindowsStyleArgumentTokenizer.getRawFirstToken("\"fun\"")); //$NON-NLS-1$ //$NON-NLS-2$
        assertEquals(" \"fun and bar\"", WindowsStyleArgumentTokenizer.getRawFirstToken(" \"fun and bar\"")); //$NON-NLS-1$ //$NON-NLS-2$

        // Preserve quotes with embedded quotes and spaces
        assertEquals("\"fun\"", WindowsStyleArgumentTokenizer.getRawFirstToken("\"fun\"")); //$NON-NLS-1$ //$NON-NLS-2$
        assertEquals(" \"fun \"\"and bar\"", WindowsStyleArgumentTokenizer.getRawFirstToken(" \"fun \"\"and bar\"")); //$NON-NLS-1$ //$NON-NLS-2$
        assertEquals(
            " \"fun \"\"and bar\"", //$NON-NLS-1$
            WindowsStyleArgumentTokenizer.getRawFirstToken(" \"fun \"\"and bar\" \t \t")); //$NON-NLS-1$
    }
}
