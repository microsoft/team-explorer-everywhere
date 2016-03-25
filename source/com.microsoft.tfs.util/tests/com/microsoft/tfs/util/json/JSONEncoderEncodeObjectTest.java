// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.util.json;

import junit.framework.TestCase;

public class JSONEncoderEncodeObjectTest extends TestCase {
    public void testThrowForNull() {
        try {
            JSONEncoder.encodeObject(null);
            fail();
        } catch (final NullPointerException e) {
        }
    }

    public void testEncodeSimple() {
        final JSONObject o = new JSONObject();
        assertEquals("{}", JSONEncoder.encodeObject(o)); //$NON-NLS-1$

        o.put("abc", "def"); //$NON-NLS-1$//$NON-NLS-2$
        assertEquals("{\"abc\":\"def\"}", JSONEncoder.encodeObject(o)); //$NON-NLS-1$

        o.put(" x ", " y "); //$NON-NLS-1$//$NON-NLS-2$

        final String[] expected = new String[] {
            "{\" x \":\" y \",\"abc\":\"def\"}", //$NON-NLS-1$
            "{\"abc\":\"def\",\" x \":\" y \"}" //$NON-NLS-1$
        };
        final String actual = JSONEncoder.encodeObject(o);

        for (final String result : expected) {
            if (result.equals(actual)) {
                return;
            }
        }

        fail();
    }

    public void testNullValue() {
        final JSONObject o = new JSONObject();

        o.put("z", null); //$NON-NLS-1$
        assertEquals("{\"z\":null}", JSONEncoder.encodeObject(o)); //$NON-NLS-1$
    }

    public void testNameAndValueEscape() {
        final JSONObject o = new JSONObject();

        // Names get escaped like all strings
        o.put("abc\ndef", "\u001F and \t"); //$NON-NLS-1$ //$NON-NLS-2$
        assertEquals("{\"abc\\ndef\":\"\\u001f and \\t\"}", JSONEncoder.encodeObject(o)); //$NON-NLS-1$
    }
}
