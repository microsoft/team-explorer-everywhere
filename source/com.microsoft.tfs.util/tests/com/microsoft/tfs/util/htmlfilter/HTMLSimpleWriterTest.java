// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.util.htmlfilter;

import junit.framework.TestCase;

public class HTMLSimpleWriterTest extends TestCase {
    public void testWrite() {
        final String html = "<html><tag /><p a=\"b\">some simple text</p></html>"; //$NON-NLS-1$
        // ..................012345678911111111.11.2222222222333333333344444444445
        // ............................01234567.89.0123456789012345678901234567890

        final HTMLSimpleWriter writer = new HTMLSimpleWriter();

        /*
         * Build from the HTML string via index and length. HtmlSimpleWriter
         * only cares about offset and length; the other params (tag, endTag,
         * attr) are not used.
         */

        // "<html>"
        writer.writeTag(html, 0, 6, null, false);

        // "<tag />"
        writer.writeTag(html, 6, 7, null, false);

        // "<p "
        writer.writeTag(html, 13, 3, null, false);

        // "a="b""
        writer.writeAttribute(html, 16, 5, null, null, 0, 0);

        // ">"
        writer.writeTag(html, 21, 1, null, false);

        // "some simple text"
        writer.writeText(html, 22, 16);

        // "</p>"
        writer.writeEndOfTag(html, 38, 4, null);

        // "</html>"
        writer.writeEndOfTag(html, 42, 7, null);

        assertEquals(html, writer.toString());
    }
}
