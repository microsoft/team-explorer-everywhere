// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.clc.vc.options;

import com.microsoft.tfs.client.clc.exceptions.InvalidOptionValueException;
import com.microsoft.tfs.client.clc.options.shared.OptionLogin;
import com.microsoft.tfs.core.util.TFSUser;

import junit.framework.TestCase;

/**
 *         Tests the UsernamePasswordValueOption, but since that class is
 *         abstract, actually uses OptionLogin.
 */
public class TestUsernamePasswordValueOption extends TestCase {
    public void testOptionLogin() throws InvalidOptionValueException {
        final String FUNKY_PASSWORD = "#(,@<#MF#@<\\,,32\\"; //$NON-NLS-1$
        final String FUNKY_PASSWORD2 = ","; //$NON-NLS-1$

        // Should fail to parse.
        final String EMPTY = ""; //$NON-NLS-1$

        // Should parse.
        final String USERNAME_NO_PASSWORD = "username"; //$NON-NLS-1$
        final String USERNAME_DOMAIN_NO_PASSWORD = "username@domain"; //$NON-NLS-1$
        final String USERNAME_DOMAIN2_NO_PASSWORD = "domain\\username"; //$NON-NLS-1$

        final String USERNAME = "username,"; //$NON-NLS-1$
        final String USERNAME_PASSWORD = "username,password"; //$NON-NLS-1$

        final String USERNAME_DOMAIN = "username@domain,"; //$NON-NLS-1$
        final String USERNAME_DOMAIN2 = "domain\\username,"; //$NON-NLS-1$
        final String USERNAME_DOMAIN_PASSWORD = "username@domain,password"; //$NON-NLS-1$
        final String USERNAME_DOMAIN2_PASSWORD = "domain\\username,password"; //$NON-NLS-1$
        final String USERNAME_DOMAIN_PASSWORD_FUNKY = "username@domain," + FUNKY_PASSWORD; //$NON-NLS-1$
        final String USERNAME_DOMAIN_PASSWORD_FUNKY2 = "username@domain," + FUNKY_PASSWORD2; //$NON-NLS-1$
        final String USERNAME_DOMAIN_PASSWORD_FUNKY3 = "domain\\username," + FUNKY_PASSWORD; //$NON-NLS-1$
        final String USERNAME_DOMAIN_PASSWORD_FUNKY4 = "domain\\username," + FUNKY_PASSWORD2; //$NON-NLS-1$

        OptionLogin o;

        // Should fail to parse.

        o = new OptionLogin();
        try {
            o.parseValues(EMPTY);
            fail("Should throw exception for missing separator."); //$NON-NLS-1$
        } catch (final InvalidOptionValueException e) {
        }

        // Should parse.

        o = new OptionLogin();
        o.parseValues(USERNAME_NO_PASSWORD);
        assertEquals(o.getUsername(), "username"); //$NON-NLS-1$
        assertNull(o.getPassword());
        assertEquals(new TFSUser(o.getUsername()).toString(), "username"); //$NON-NLS-1$

        o = new OptionLogin();
        o.parseValues(USERNAME);
        assertEquals(o.getUsername(), "username"); //$NON-NLS-1$
        assertNotNull(o.getPassword());
        assertEquals(o.getPassword().length(), 0);
        assertEquals(new TFSUser(o.getUsername()).toString(), "username"); //$NON-NLS-1$

        o = new OptionLogin();
        o.parseValues(USERNAME_PASSWORD);
        assertEquals(o.getUsername(), "username"); //$NON-NLS-1$
        assertEquals(o.getPassword(), "password"); //$NON-NLS-1$
        assertEquals(new TFSUser(o.getUsername()).toString(), "username"); //$NON-NLS-1$

        o = new OptionLogin();
        o.parseValues(USERNAME_DOMAIN_NO_PASSWORD);
        assertEquals(o.getUsername(), "username@domain"); //$NON-NLS-1$
        assertNull(o.getPassword());
        assertEquals(new TFSUser(o.getUsername()).toString(), "domain\\username"); //$NON-NLS-1$

        o = new OptionLogin();
        o.parseValues(USERNAME_DOMAIN2_NO_PASSWORD);
        assertEquals(o.getUsername(), "domain\\username"); //$NON-NLS-1$
        assertNull(o.getPassword());
        assertEquals(new TFSUser(o.getUsername()).toString(), "domain\\username"); //$NON-NLS-1$

        o = new OptionLogin();
        o.parseValues(USERNAME_DOMAIN);
        assertEquals(o.getUsername(), "username@domain"); //$NON-NLS-1$
        assertNotNull(o.getPassword());
        assertEquals(o.getPassword().length(), 0);
        assertEquals(new TFSUser(o.getUsername()).toString(), "domain\\username"); //$NON-NLS-1$

        o = new OptionLogin();
        o.parseValues(USERNAME_DOMAIN2);
        assertEquals(o.getUsername(), "domain\\username"); //$NON-NLS-1$
        assertNotNull(o.getPassword());
        assertEquals(o.getPassword().length(), 0);
        assertEquals(new TFSUser(o.getUsername()).toString(), "domain\\username"); //$NON-NLS-1$

        o = new OptionLogin();
        o.parseValues(USERNAME_DOMAIN_PASSWORD);
        assertEquals(o.getUsername(), "username@domain"); //$NON-NLS-1$
        assertEquals(o.getPassword(), "password"); //$NON-NLS-1$
        assertEquals(new TFSUser(o.getUsername()).toString(), "domain\\username"); //$NON-NLS-1$

        o = new OptionLogin();
        o.parseValues(USERNAME_DOMAIN2_PASSWORD);
        assertEquals(o.getUsername(), "domain\\username"); //$NON-NLS-1$
        assertEquals(o.getPassword(), "password"); //$NON-NLS-1$
        assertEquals(new TFSUser(o.getUsername()).toString(), "domain\\username"); //$NON-NLS-1$

        o = new OptionLogin();
        o.parseValues(USERNAME_DOMAIN_PASSWORD_FUNKY);
        assertEquals(o.getUsername(), "username@domain"); //$NON-NLS-1$
        assertEquals(o.getPassword(), FUNKY_PASSWORD);
        assertEquals(new TFSUser(o.getUsername()).toString(), "domain\\username"); //$NON-NLS-1$

        o = new OptionLogin();
        o.parseValues(USERNAME_DOMAIN_PASSWORD_FUNKY2);
        assertEquals(o.getUsername(), "username@domain"); //$NON-NLS-1$
        assertEquals(o.getPassword(), FUNKY_PASSWORD2);
        assertEquals(new TFSUser(o.getUsername()).toString(), "domain\\username"); //$NON-NLS-1$

        o = new OptionLogin();
        o.parseValues(USERNAME_DOMAIN_PASSWORD_FUNKY3);
        assertEquals(o.getUsername(), "domain\\username"); //$NON-NLS-1$
        assertEquals(o.getPassword(), FUNKY_PASSWORD);
        assertEquals(new TFSUser(o.getUsername()).toString(), "domain\\username"); //$NON-NLS-1$

        o = new OptionLogin();
        o.parseValues(USERNAME_DOMAIN_PASSWORD_FUNKY4);
        assertEquals(o.getUsername(), "domain\\username"); //$NON-NLS-1$
        assertEquals(o.getPassword(), FUNKY_PASSWORD2);
        assertEquals(new TFSUser(o.getUsername()).toString(), "domain\\username"); //$NON-NLS-1$

        // Test some object re-use.

        o = new OptionLogin();
        o.parseValues("username@domain,password"); //$NON-NLS-1$
        assertEquals(o.getUsername(), "username@domain"); //$NON-NLS-1$
        assertEquals(o.getPassword(), "password"); //$NON-NLS-1$
        assertEquals(new TFSUser(o.getUsername()).toString(), "domain\\username"); //$NON-NLS-1$
        o.parseValues("a@b,c"); //$NON-NLS-1$
        assertEquals(o.getUsername(), "a@b"); //$NON-NLS-1$
        assertEquals(o.getPassword(), "c"); //$NON-NLS-1$
        assertEquals(new TFSUser(o.getUsername()).toString(), "b\\a"); //$NON-NLS-1$
        o.parseValues("a@b,"); //$NON-NLS-1$
        assertEquals(o.getUsername(), "a@b"); //$NON-NLS-1$
        assertNotNull(o.getPassword());
        assertEquals(o.getPassword().length(), 0);
        assertEquals(new TFSUser(o.getUsername()).toString(), "b\\a"); //$NON-NLS-1$
    }
}
