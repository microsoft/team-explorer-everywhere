// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.util.json;

import junit.framework.TestCase;

public class JSONEncoderDecodeObjectTest extends TestCase {
    public void testThrowForNull() {
        try {
            JSONEncoder.decodeObject(null);
            fail();
        } catch (final NullPointerException e) {
        }
    }

    public void testDecodeSimple() {
        JSONObject o;

        o = JSONEncoder.decodeObject("{}"); //$NON-NLS-1$
        assertNotNull(o);
        assertEquals(0, o.size());

        o = JSONEncoder.decodeObject("{\"fun\":\"bar\"}"); //$NON-NLS-1$
        assertNotNull(o);
        assertEquals(1, o.size());
        assertEquals("bar", o.get("fun")); //$NON-NLS-1$//$NON-NLS-2$

        o = JSONEncoder.decodeObject("{\"fun\":\"bar\",\" x \":\" y \"}"); //$NON-NLS-1$
        assertNotNull(o);
        assertEquals(2, o.size());
        assertEquals("bar", o.get("fun")); //$NON-NLS-1$//$NON-NLS-2$
        assertEquals(" y ", o.get(" x ")); //$NON-NLS-1$//$NON-NLS-2$
    }

    public void testDecodeLotsOfWhitespace() {
        JSONObject o;

        o = JSONEncoder.decodeObject(
            "\r\n\t { \r\n\t \"fun\" \r\n\t : \r\n\t  \"bar\" \r\n\t , \r\n\t \" x \" \r\n\t : \r\n\t \" y \" \r\n\t } \r\n\t"); //$NON-NLS-1$
        assertNotNull(o);
        assertEquals(2, o.size());
        assertEquals("bar", o.get("fun")); //$NON-NLS-1$//$NON-NLS-2$
        assertEquals(" y ", o.get(" x ")); //$NON-NLS-1$//$NON-NLS-2$
    }

    public void testNullValue() {
        JSONObject o;

        o = JSONEncoder.decodeObject("{\"fun\":null}"); //$NON-NLS-1$
        assertNotNull(o);
        assertEquals(1, o.size());
        assertNull(o.get("fun")); //$NON-NLS-1$
    }
}
