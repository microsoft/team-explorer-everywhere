// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.util.htmlfilter;

import java.io.IOException;
import java.io.InputStreamReader;

import junit.framework.TestCase;

public class HTMLFilterTest extends TestCase {
    public void testWrite() throws IOException {
        /*
         * If you edit unsafe.html, make sure to edit stripped.html to match.
         */
        final String unsafeHtml = getResourceString("unsafe.html"); //$NON-NLS-1$
        final String strippedHtml = getResourceString("stripped.html"); //$NON-NLS-1$

        assertEquals(strippedHtml, HTMLFilter.strip(unsafeHtml));
    }

    final String getResourceString(final String resourceName) throws IOException {
        final InputStreamReader reader = new InputStreamReader(HTMLFilterTest.class.getResourceAsStream(resourceName));
        final StringBuilder sb = new StringBuilder();

        int character;
        while ((character = reader.read()) != -1) {
            sb.append((char) character);
        }

        reader.close();

        return sb.toString();
    }
}
