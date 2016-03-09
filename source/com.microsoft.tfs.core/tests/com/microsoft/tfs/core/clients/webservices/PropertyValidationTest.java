// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.webservices;

import java.util.Calendar;
import java.util.GregorianCalendar;

import junit.framework.TestCase;

public class PropertyValidationTest extends TestCase {
    public void testValidatePropertyName() {
        PropertyValidation.validatePropertyName("a"); //$NON-NLS-1$
        PropertyValidation.validatePropertyName("some longer string"); //$NON-NLS-1$
        PropertyValidation.validatePropertyName(makeString(PropertyValidation.MAX_PROPERTY_NAME_LENGTH_IN_CHARS));

        try {
            PropertyValidation.validatePropertyName(" a"); //$NON-NLS-1$
            assertTrue("can't have leading whitespace", false); //$NON-NLS-1$
        } catch (final TeamFoundationPropertyValidationException e) {
        }

        try {
            PropertyValidation.validatePropertyName("a "); //$NON-NLS-1$
            assertTrue("can't have trailing whitespace", false); //$NON-NLS-1$
        } catch (final TeamFoundationPropertyValidationException e) {
        }

        try {
            PropertyValidation.validatePropertyName(
                makeString(PropertyValidation.MAX_PROPERTY_NAME_LENGTH_IN_CHARS + 1));
            assertTrue("name is too long", false); //$NON-NLS-1$
        } catch (final TeamFoundationPropertyValidationException e) {
        }

    }

    public void testValidatePropertyValue_ByteArray() {
        PropertyValidation.validatePropertyValue("a", new byte[0]); //$NON-NLS-1$
        PropertyValidation.validatePropertyValue("a", new byte[] { //$NON-NLS-1$
            0x00
        });
        PropertyValidation.validatePropertyValue("a", new byte[] { //$NON-NLS-1$
            Byte.MIN_VALUE,
            Byte.MAX_VALUE
        });
        PropertyValidation.validatePropertyValue("a", new byte[PropertyValidation.MAX_BYTE_VALUE_SIZE]); //$NON-NLS-1$

        try {
            PropertyValidation.validatePropertyValue("a", new byte[PropertyValidation.MAX_BYTE_VALUE_SIZE + 1]); //$NON-NLS-1$
            assertTrue("value is too long", false); //$NON-NLS-1$
        } catch (final TeamFoundationPropertyValidationException e) {
        }
    }

    public void testValidatePropertyValue_UnsupportedArray() {
        // The only array type supported is byte[]

        try {
            PropertyValidation.validatePropertyValue("a", new Object[] { //$NON-NLS-1$
                new Object()
            });
            assertTrue("doesn't support non-byte arrays", false); //$NON-NLS-1$
        } catch (final TeamFoundationPropertyValidationException e) {
        }

        try {
            PropertyValidation.validatePropertyValue(
                "a", //$NON-NLS-1$
                new Integer[] {
                    1,
                    2
            });
            assertTrue("doesn't support non-byte arrays", false); //$NON-NLS-1$
        } catch (final TeamFoundationPropertyValidationException e) {
        }

        try {
            PropertyValidation.validatePropertyValue(
                "a", //$NON-NLS-1$
                new Double[] {
                    1.0,
                    2.0
            });
            assertTrue("doesn't support non-byte arrays", false); //$NON-NLS-1$
        } catch (final TeamFoundationPropertyValidationException e) {
        }
    }

    public void testValidatePropertyValue_Integer() {
        PropertyValidation.validatePropertyValue("a", 0); //$NON-NLS-1$
        PropertyValidation.validatePropertyValue("a", new Integer(1)); //$NON-NLS-1$
    }

    public void testValidatePropertyValue_Double() {
        PropertyValidation.validatePropertyValue("a", 0.0); //$NON-NLS-1$
        PropertyValidation.validatePropertyValue("a", new Double(1.0)); //$NON-NLS-1$
        PropertyValidation.validatePropertyValue("a", PropertyValidation.MIN_POSITIVE); //$NON-NLS-1$
        PropertyValidation.validatePropertyValue("a", PropertyValidation.MAX_POSITIVE); //$NON-NLS-1$
        PropertyValidation.validatePropertyValue("a", PropertyValidation.MIN_NEGATIVE); //$NON-NLS-1$
        PropertyValidation.validatePropertyValue("a", PropertyValidation.MAX_NEGATIVE); //$NON-NLS-1$

        try {
            // Java's Double.MIN_VALUE is smaller than
            // PropertyValidation.MIN_POSITIVE
            PropertyValidation.validatePropertyValue("a", Double.MIN_VALUE); //$NON-NLS-1$
            assertTrue("too small positive value", false); //$NON-NLS-1$
        } catch (final TeamFoundationPropertyValidationException e) {
        }

        try {
            // Java's Double.MAX_VALUE is larger than
            // PropertyValidation.MAX_POSITIVE
            PropertyValidation.validatePropertyValue("a", Double.MAX_VALUE); //$NON-NLS-1$
            assertTrue("too large positive value", false); //$NON-NLS-1$
        } catch (final TeamFoundationPropertyValidationException e) {
        }

        try {
            // Java's Double.MIN_VALUE * -1 is smaller than
            // PropertyValidation.MIN_NEGATIVE
            PropertyValidation.validatePropertyValue("a", Double.MIN_VALUE * -1); //$NON-NLS-1$
            assertTrue("too small negative value", false); //$NON-NLS-1$
        } catch (final TeamFoundationPropertyValidationException e) {
        }

        try {
            // Java's Double.MAX_VALUE * -1 is larger than
            // PropertyValidation.MAX_NEGATIVE
            PropertyValidation.validatePropertyValue("a", Double.MAX_VALUE * -1); //$NON-NLS-1$
            assertTrue("too large negative value", false); //$NON-NLS-1$
        } catch (final TeamFoundationPropertyValidationException e) {
        }

        try {
            PropertyValidation.validatePropertyValue("a", Double.NaN); //$NON-NLS-1$
            assertTrue("doesn't support NaN", false); //$NON-NLS-1$
        } catch (final TeamFoundationPropertyValidationException e) {
        }

        try {
            PropertyValidation.validatePropertyValue("a", Double.NEGATIVE_INFINITY); //$NON-NLS-1$
            assertTrue("doesn't support infinity", false); //$NON-NLS-1$
        } catch (final TeamFoundationPropertyValidationException e) {
        }

        try {
            PropertyValidation.validatePropertyValue("a", Double.POSITIVE_INFINITY); //$NON-NLS-1$
            assertTrue("doesn't support infinity", false); //$NON-NLS-1$
        } catch (final TeamFoundationPropertyValidationException e) {
        }

    }

    public void testValidatePropertyValue_Calendar() {
        PropertyValidation.validatePropertyValue("a", Calendar.getInstance()); //$NON-NLS-1$
        PropertyValidation.validatePropertyValue("a", new GregorianCalendar(2011, Calendar.SEPTEMBER, 27, 15, 25, 43)); //$NON-NLS-1$
        PropertyValidation.validatePropertyValue("a", PropertyValidation.MIN_ALLOWED_DATE_TIME); //$NON-NLS-1$
        PropertyValidation.validatePropertyValue("a", PropertyValidation.MAX_ALLOWED_DATE_TIME); //$NON-NLS-1$

        // One second too old
        try {
            final Calendar c = (Calendar) PropertyValidation.MIN_ALLOWED_DATE_TIME.clone();
            c.add(Calendar.SECOND, -1);
            PropertyValidation.validatePropertyValue("a", c); //$NON-NLS-1$
            assertTrue("date too old", false); //$NON-NLS-1$
        } catch (final IllegalArgumentException e) {
        }

        // One year too old
        try {
            final Calendar c = (Calendar) PropertyValidation.MIN_ALLOWED_DATE_TIME.clone();
            c.add(Calendar.YEAR, -1);
            PropertyValidation.validatePropertyValue("a", c); //$NON-NLS-1$
            assertTrue("date too old", false); //$NON-NLS-1$
        } catch (final IllegalArgumentException e) {
        }

        // One second too new
        try {
            final Calendar c = (Calendar) PropertyValidation.MAX_ALLOWED_DATE_TIME.clone();
            c.add(Calendar.SECOND, 1);
            PropertyValidation.validatePropertyValue("a", c); //$NON-NLS-1$
            assertTrue("date too new", false); //$NON-NLS-1$
        } catch (final IllegalArgumentException e) {
        }

        // One year too new
        try {
            final Calendar c = (Calendar) PropertyValidation.MAX_ALLOWED_DATE_TIME.clone();
            c.add(Calendar.YEAR, 1);
            PropertyValidation.validatePropertyValue("a", c); //$NON-NLS-1$
            assertTrue("date too new", false); //$NON-NLS-1$
        } catch (final IllegalArgumentException e) {
        }
    }

    public void testValidatePropertyValue_String() {
        PropertyValidation.validatePropertyValue("a", ""); //$NON-NLS-1$//$NON-NLS-2$
        PropertyValidation.validatePropertyValue("a", "string"); //$NON-NLS-1$//$NON-NLS-2$
        PropertyValidation.validatePropertyValue("a", "a longer string"); //$NON-NLS-1$//$NON-NLS-2$
        PropertyValidation.validatePropertyValue("a", makeString(PropertyValidation.MAX_STRING_VALUE_LENGTH)); //$NON-NLS-1$

        try {
            PropertyValidation.validatePropertyValue("a", makeString(PropertyValidation.MAX_STRING_VALUE_LENGTH + 1)); //$NON-NLS-1$
            assertTrue("value is too long", false); //$NON-NLS-1$
        } catch (final TeamFoundationPropertyValidationException e) {
        }

        try {
            PropertyValidation.validatePropertyValue("a", "a\u0001b"); //$NON-NLS-1$//$NON-NLS-2$
            assertTrue("value has control characters", false); //$NON-NLS-1$
        } catch (final IllegalArgumentException e) {
        }
    }

    public void testValidatePropertyValue_Misc() {
        /*
         * The VS implementation converts these to strings and validates them as
         * such: Boolean Char SByte Byte Int16 UInt32 UInt16 Int64 UInt64 Single
         * Decimal
         */

        PropertyValidation.validatePropertyValue("a", Boolean.TRUE); //$NON-NLS-1$
        PropertyValidation.validatePropertyValue("a", 'x'); //$NON-NLS-1$
        PropertyValidation.validatePropertyValue("a", Byte.MIN_VALUE); //$NON-NLS-1$
        PropertyValidation.validatePropertyValue("a", Byte.MAX_VALUE); //$NON-NLS-1$
        PropertyValidation.validatePropertyValue("a", Short.MIN_VALUE); //$NON-NLS-1$
        PropertyValidation.validatePropertyValue("a", Short.MIN_VALUE); //$NON-NLS-1$
        PropertyValidation.validatePropertyValue("a", Integer.MIN_VALUE); //$NON-NLS-1$
        PropertyValidation.validatePropertyValue("a", Integer.MIN_VALUE); //$NON-NLS-1$
        PropertyValidation.validatePropertyValue("a", Long.MIN_VALUE); //$NON-NLS-1$
        PropertyValidation.validatePropertyValue("a", Long.MAX_VALUE); //$NON-NLS-1$
        PropertyValidation.validatePropertyValue("a", Float.MIN_VALUE); //$NON-NLS-1$
        PropertyValidation.validatePropertyValue("a", Float.MAX_VALUE); //$NON-NLS-1$
    }

    private String makeString(final int length) {
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append('a');
        }
        return sb.toString();
    }
}
