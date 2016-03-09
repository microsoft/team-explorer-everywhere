// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.clc.vc.options;

import com.microsoft.tfs.client.clc.exceptions.InvalidOptionValueException;
import com.microsoft.tfs.client.clc.options.shared.OptionUser;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.LockLevel;
import com.microsoft.tfs.core.util.FileEncoding;

import junit.framework.TestCase;

/**
 *         Tests a SingleValueOption class, but since that class is abstract,
 *         actually uses a few derived classes.
 */
public class TestSingleValueOption extends TestCase {
    public void testOptionUser() throws InvalidOptionValueException {
        OptionUser o;

        // Verify that this kind of single value option doesn't expect
        // any specific strings.
        o = new OptionUser();
        assertNull(o.getValidOptionValues());

        // Should fail to parse.

        o = new OptionUser();
        try {
            o.parseValues(null);
            fail("Should throw exception for null value."); //$NON-NLS-1$
        } catch (final InvalidOptionValueException e) {
        }

        // Should parse.

        o = new OptionUser();
        o.parseValues(""); //$NON-NLS-1$
        assertEquals("", o.getValue()); //$NON-NLS-1$

        o = new OptionUser();
        o.parseValues("word"); //$NON-NLS-1$
        assertEquals("word", o.getValue()); //$NON-NLS-1$

        o = new OptionUser();
        o.parseValues("big long phrase with spaces and things"); //$NON-NLS-1$
        assertEquals("big long phrase with spaces and things", o.getValue()); //$NON-NLS-1$

        o = new OptionUser();
        final String GARBAGE = ",32\\@MF(7,-=*&*)_)<\"'>:}?&&N"; //$NON-NLS-1$
        o.parseValues(GARBAGE);
        assertEquals(GARBAGE, o.getValue());
    }

    public void testOptionLock() throws InvalidOptionValueException {
        OptionLock o;

        // Verify that this kind of single value option doesn't expect
        // any specific strings.
        o = new OptionLock();
        assertNotNull(o.getValidOptionValues());
        assertEquals(o.getValidOptionValues().length, 3);

        // Should fail to parse.

        o = new OptionLock();
        try {
            o.parseValues(null);
            fail("Should throw exception for null value."); //$NON-NLS-1$
        } catch (final InvalidOptionValueException e) {
        }

        o = new OptionLock();
        try {
            o.parseValues("invalid value ahoy!"); //$NON-NLS-1$
            fail("Should throw exception for invalid option value."); //$NON-NLS-1$
        } catch (final InvalidOptionValueException e) {
        }

        o = new OptionLock();
        try {
            o.parseValues("none "); //$NON-NLS-1$
            fail("Should throw exception for invalid option value (trailing space)"); //$NON-NLS-1$
        } catch (final InvalidOptionValueException e) {
        }

        // Should parse.

        o = new OptionLock();
        o.parseValues("none"); //$NON-NLS-1$
        assertEquals(LockLevel.NONE, o.getValueAsLockLevel());

        o = new OptionLock();
        o.parseValues("NONe"); //$NON-NLS-1$
        assertEquals(LockLevel.NONE, o.getValueAsLockLevel());

        o = new OptionLock();
        o.parseValues("checkin"); //$NON-NLS-1$
        assertEquals(LockLevel.CHECKIN, o.getValueAsLockLevel());

        o = new OptionLock();
        o.parseValues("CHECKIN"); //$NON-NLS-1$
        assertEquals(LockLevel.CHECKIN, o.getValueAsLockLevel());

        o = new OptionLock();
        o.parseValues("checkout"); //$NON-NLS-1$
        assertEquals(LockLevel.CHECKOUT, o.getValueAsLockLevel());

        o = new OptionLock();
        o.parseValues("CheCKoUt"); //$NON-NLS-1$
        assertEquals(LockLevel.CHECKOUT, o.getValueAsLockLevel());
    }

    public void testOptionType() throws InvalidOptionValueException {
        OptionType o;

        o = new OptionType();
        assertNull(o.getValidOptionValues());

        // Should fail to parse.

        o = new OptionType();
        try {
            o.parseValues(null);
            fail("Should throw exception for null value."); //$NON-NLS-1$
        } catch (final InvalidOptionValueException e) {
        }

        // Should parse.

        o = new OptionType();
        o.parseValues("binary"); //$NON-NLS-1$
        assertEquals(FileEncoding.BINARY, o.getValueAsEncoding());

        o = new OptionType();
        o.parseValues("text"); //$NON-NLS-1$
        assertEquals(FileEncoding.DEFAULT_TEXT, o.getValueAsEncoding());

        o = new OptionType();
        o.parseValues("auto"); //$NON-NLS-1$
        assertEquals(FileEncoding.AUTOMATICALLY_DETECT, o.getValueAsEncoding());

        // UTF 8
        o = new OptionType();
        o.parseValues("utf-8"); //$NON-NLS-1$
        assertEquals(FileEncoding.UTF_8, o.getValueAsEncoding());
        o.parseValues("UTF-8"); //$NON-NLS-1$
        assertEquals(FileEncoding.UTF_8, o.getValueAsEncoding());
        o.parseValues("utf8"); //$NON-NLS-1$
        assertEquals(FileEncoding.UTF_8, o.getValueAsEncoding());

        // UTF 16 (LE)
        o = new OptionType();
        o.parseValues("utf-16"); //$NON-NLS-1$
        assertEquals(FileEncoding.UTF_16, o.getValueAsEncoding());
        o.parseValues("utf-16"); //$NON-NLS-1$
        assertEquals(FileEncoding.UTF_16, o.getValueAsEncoding());
        o.parseValues("UTF-16"); //$NON-NLS-1$
        assertEquals(FileEncoding.UTF_16, o.getValueAsEncoding());
        o.parseValues("utf16"); //$NON-NLS-1$
        assertEquals(FileEncoding.UTF_16, o.getValueAsEncoding());

        // UTF 16 BE
        o = new OptionType();
        o.parseValues("utf-16BE"); //$NON-NLS-1$
        assertEquals(FileEncoding.UTF_16BE, o.getValueAsEncoding());
        o.parseValues("utf-16be"); //$NON-NLS-1$
        assertEquals(FileEncoding.UTF_16BE, o.getValueAsEncoding());
        o.parseValues("UTF-16Be"); //$NON-NLS-1$
        assertEquals(FileEncoding.UTF_16BE, o.getValueAsEncoding());

        // UTF 32 (LE)
        o = new OptionType();
        o.parseValues("utf-32"); //$NON-NLS-1$
        assertEquals(FileEncoding.UTF_32, o.getValueAsEncoding());
        o.parseValues("utf-32"); //$NON-NLS-1$
        assertEquals(FileEncoding.UTF_32, o.getValueAsEncoding());
        o.parseValues("UTF-32"); //$NON-NLS-1$
        assertEquals(FileEncoding.UTF_32, o.getValueAsEncoding());

        // UTF 32 BE
        o = new OptionType();
        o.parseValues("utf-32BE"); //$NON-NLS-1$
        assertEquals(FileEncoding.UTF_32BE, o.getValueAsEncoding());
        o.parseValues("utf-32be"); //$NON-NLS-1$
        assertEquals(FileEncoding.UTF_32BE, o.getValueAsEncoding());
        o.parseValues("UTF-32Be"); //$NON-NLS-1$
        assertEquals(FileEncoding.UTF_32BE, o.getValueAsEncoding());

        // Windows 1252
        o = new OptionType();

        // Unknown number: a number not in the table should be binary.
        o = new OptionType();
        o.parseValues("1252"); //$NON-NLS-1$
        assertEquals(new FileEncoding(1252), o.getValueAsEncoding());
        o.parseValues("666777111888"); //$NON-NLS-1$
        assertEquals(FileEncoding.BINARY, o.getValueAsEncoding());

        // Not an integer: should be binary.
        o = new OptionType();
        o.parseValues("notaninteger"); //$NON-NLS-1$
        assertEquals(FileEncoding.BINARY, o.getValueAsEncoding());
    }
}
