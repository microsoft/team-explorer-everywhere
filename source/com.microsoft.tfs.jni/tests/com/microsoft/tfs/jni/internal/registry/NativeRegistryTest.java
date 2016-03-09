// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.jni.internal.registry;

import com.microsoft.tfs.jni.RegistryKey;
import com.microsoft.tfs.jni.RootKey;
import com.microsoft.tfs.util.Platform;

import junit.framework.TestCase;

public class NativeRegistryTest extends TestCase {
    public void testHasSubkeys() {
        if (Platform.isCurrentPlatform(Platform.WINDOWS)) {
            RegistryKey key;

            // test one that has more than one subkey in HKCU.
            key = new RegistryKey(RootKey.HKEY_CURRENT_USER, "AppEvents"); //$NON-NLS-1$
            assertTrue(key.hasSubkeys());

            // test one that has no subkeys in HKCU.
            key = new RegistryKey(RootKey.HKEY_CURRENT_USER, "Environment"); //$NON-NLS-1$
            assertFalse(key.hasSubkeys());

            // test an invalid parent subkey in HKCU.
            key = new RegistryKey(RootKey.HKEY_CURRENT_USER, "Console123456"); //$NON-NLS-1$
            assertFalse(key.hasSubkeys());

            // test one that has subkeys in root HKLM.
            key = new RegistryKey(RootKey.HKEY_LOCAL_MACHINE, "SAM"); //$NON-NLS-1$
            assertTrue(key.hasSubkeys());
        }
    }

    public void testHasSubkey() {
        if (Platform.isCurrentPlatform(Platform.WINDOWS)) {
            RegistryKey key;

            // test a subkey that exists in HKCU.
            key = new RegistryKey(RootKey.HKEY_CURRENT_USER, "AppEvents"); //$NON-NLS-1$
            assertTrue(key.hasSubkey("Schemes")); //$NON-NLS-1$

            // test a subkey that does not exist in HKCU.
            key = new RegistryKey(RootKey.HKEY_CURRENT_USER, "AppEvents"); //$NON-NLS-1$
            assertFalse(key.hasSubkey("Schemes1")); //$NON-NLS-1$

            // test a parnent subkey that does not exist in HKCU.
            key = new RegistryKey(RootKey.HKEY_CURRENT_USER, "AppEvents1"); //$NON-NLS-1$
            assertFalse(key.hasSubkey("Schemes")); //$NON-NLS-1$

            // test one that has subkeys in root HKLM.
            key = new RegistryKey(RootKey.HKEY_LOCAL_MACHINE, "HARDWARE"); //$NON-NLS-1$
            assertTrue(key.hasSubkey("DEVICEMAP")); //$NON-NLS-1$
        }
    }

    public void testHasValues() {
        if (Platform.isCurrentPlatform(Platform.WINDOWS)) {
            RegistryKey key;

            // test a subkey that has > 1 values in HKCU.
            key = new RegistryKey(RootKey.HKEY_CURRENT_USER, "Console"); //$NON-NLS-1$
            assertTrue(key.hasValues());

            // test a subkey that has 0 values in HKCU.
            key = new RegistryKey(RootKey.HKEY_CURRENT_USER, "AppEvents"); //$NON-NLS-1$
            assertFalse(key.hasValues());
        }
    }

    public void testHasValue() {
        if (Platform.isCurrentPlatform(Platform.WINDOWS)) {
            RegistryKey key;

            // test a valid value name with a DWORD value in HKCU.
            key = new RegistryKey(RootKey.HKEY_CURRENT_USER, "Console"); //$NON-NLS-1$
            assertTrue(key.hasValue("InsertMode")); //$NON-NLS-1$

            // test a valid value name with a SZ value in HKCU.
            key = new RegistryKey(RootKey.HKEY_CURRENT_USER, "Identities"); //$NON-NLS-1$
            assertTrue(key.hasValue("Last Username")); //$NON-NLS-1$

            // test an invalid value name with a SZ value in HKCU.
            key = new RegistryKey(RootKey.HKEY_CURRENT_USER, "Identities"); //$NON-NLS-1$
            assertFalse(key.hasValue("Last Username1")); //$NON-NLS-1$
        }
    }

    public void testGetIntegerValue() {
        if (Platform.isCurrentPlatform(Platform.WINDOWS)) {
            RegistryKey key;

            // Test convenience method which default to integer if DWORD not
            // found.
            key = new RegistryKey(RootKey.HKEY_CURRENT_USER, "Console"); //$NON-NLS-1$
            assertEquals(1, key.getIntegerValue("InsertMode", 222)); //$NON-NLS-1$

            // Test convenience method which default to integer if DWORD not
            // found.
            assertEquals(222, key.getIntegerValue("ColorTable022", 222)); //$NON-NLS-1$
        }
    }

    public void testGetDefaultValue() {
        if (Platform.isCurrentPlatform(Platform.WINDOWS)) {
            RegistryKey key;

            key = new RegistryKey(RootKey.HKEY_CURRENT_USER, "Software\\Microsoft\\NonExistantPath"); //$NON-NLS-1$
            assertEquals(null, key.getStringValue("", null)); //$NON-NLS-1$
            assertEquals("SomeStringDefault", key.getStringValue("", "SomeStringDefault")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        }
    }

    public void testGetStringValue() {
        if (Platform.isCurrentPlatform(Platform.WINDOWS)) {
            RegistryKey key;

            // Test convenience method which default to String if String not
            // found.
            key = new RegistryKey(RootKey.HKEY_CURRENT_USER, "Control Panel\\TimeOut"); //$NON-NLS-1$
            assertNotSame(key.getStringValue("TimeToWait", "DEFAULT"), "300000"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

            // Test convenience method which default to String if String not
            // found.
            assertEquals("DEFAULT", key.getStringValue("Last User ID1", "DEFAULT")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        }
    }
}
