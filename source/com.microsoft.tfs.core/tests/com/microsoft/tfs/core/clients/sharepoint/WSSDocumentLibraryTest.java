// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.sharepoint;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class WSSDocumentLibraryTest {

    @Test
    public void testCalculateASCIINameFromWebFullURL() {
        final WSSDocumentLibrary docLib = new WSSDocumentLibrary();
        String asciiName =
            docLib.calculateASCIINameFromWebFullURL("/sites/TEE/TEE/Reports/Forms/AllItems.aspx", "/sites/TEE/TEE"); //$NON-NLS-1$//$NON-NLS-2$
        assertEquals("Reports", asciiName); //$NON-NLS-1$

        asciiName = docLib.calculateASCIINameFromWebFullURL("/sites/TEE/TEE/Reports", "/sites/TEE/TEE"); //$NON-NLS-1$//$NON-NLS-2$
        assertEquals("Reports", asciiName); //$NON-NLS-1$

        asciiName = docLib.calculateASCIINameFromWebFullURL("/sites/TEE/TEE/Reports", "/sites/TEE/TEE/"); //$NON-NLS-1$//$NON-NLS-2$
        assertEquals("Reports", asciiName); //$NON-NLS-1$

        asciiName = docLib.calculateASCIINameFromWebFullURL("/sites/TEE/TEE/Reports/", "/sites/TEE/TEE"); //$NON-NLS-1$//$NON-NLS-2$
        assertEquals("Reports", asciiName); //$NON-NLS-1$

        asciiName = docLib.calculateASCIINameFromWebFullURL("/sites/TEE/TEE/Reports/", "/sites/TEE/TEE/"); //$NON-NLS-1$//$NON-NLS-2$
        assertEquals("Reports", asciiName); //$NON-NLS-1$

        asciiName = docLib.calculateASCIINameFromWebFullURL("/food", "/b"); //$NON-NLS-1$//$NON-NLS-2$
        assertEquals(null, asciiName);

        asciiName = docLib.calculateASCIINameFromWebFullURL(null, "/b"); //$NON-NLS-1$
        assertEquals(null, asciiName);

        asciiName = docLib.calculateASCIINameFromWebFullURL("/bar", null); //$NON-NLS-1$
        assertEquals(null, asciiName);
    }

}
