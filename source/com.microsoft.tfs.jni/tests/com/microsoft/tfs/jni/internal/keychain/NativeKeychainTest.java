// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.jni.internal.keychain;

import com.microsoft.tfs.jni.AllNativeTests;
import com.microsoft.tfs.jni.KeychainAuthenticationType;
import com.microsoft.tfs.jni.KeychainInternetPassword;
import com.microsoft.tfs.jni.KeychainProtocol;
import com.microsoft.tfs.util.Platform;

import junit.framework.TestCase;

/**
 * Keychain methods test.
 *
 * @threadsafety unknown
 */
public class NativeKeychainTest extends TestCase {
    private NativeKeychain nativeImpl;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        if (Platform.isCurrentPlatform(Platform.MAC_OS_X)) {
            this.nativeImpl = new NativeKeychain();
        }
    }

    public void testKeychain() throws Exception {
        /**
         * Keychain needs UI to pop up the unlock dialog.
         */
        if (nativeImpl == null || AllNativeTests.interactiveTestsDisabled()) {
            return;
        }

        /* Create a new keychain entry. */
        final KeychainInternetPassword password = new KeychainInternetPassword();
        password.setProtocol(KeychainProtocol.HTTP);
        password.setServerName("keychain-methods-test"); //$NON-NLS-1$
        password.setPort(8080);
        password.setPath("/test"); //$NON-NLS-1$
        password.setAccountName("keychain-user"); //$NON-NLS-1$
        password.setPassword("secret".getBytes("US-ASCII")); //$NON-NLS-1$ //$NON-NLS-2$

        /* Add it to the keychain */
        assertTrue(nativeImpl.addInternetPassword(password, false));

        /* Create a search entry. */
        final KeychainInternetPassword searchPassword = new KeychainInternetPassword();
        searchPassword.setServerName("keychain-methods-test"); //$NON-NLS-1$

        final KeychainInternetPassword retrievedPassword = nativeImpl.findInternetPassword(searchPassword, false);
        assertNotNull(retrievedPassword);

        assertEquals(retrievedPassword.getProtocol(), KeychainProtocol.HTTP);
        assertEquals(retrievedPassword.getServerName(), "keychain-methods-test"); //$NON-NLS-1$
        assertEquals(retrievedPassword.getPort(), 8080);
        assertEquals(retrievedPassword.getPath(), "/test"); //$NON-NLS-1$
        assertEquals(retrievedPassword.getAccountName(), "keychain-user"); //$NON-NLS-1$
        assertArrayEquals(retrievedPassword.getPassword(), "secret".getBytes("US-ASCII")); //$NON-NLS-1$ //$NON-NLS-2$
        assertEquals(retrievedPassword.getAuthenticationType(), KeychainAuthenticationType.ANY);
        assertNull(retrievedPassword.getComment());
        assertNotNull(retrievedPassword.getLabel());
        assertNull(retrievedPassword.getID());

        /* Delete the entry. */
        assertTrue(nativeImpl.removeInternetPassword(searchPassword, false));

        /* Ensure the entry is missing. */
        assertNull(nativeImpl.findInternetPassword(searchPassword, false));
    }

    private static void assertArrayEquals(final byte[] array1, final byte[] array2) {
        if (array1 == array2) {
            return;
        }

        assertEquals(array1.length, array2.length);

        for (int i = 0; i < array1.length; i++) {
            assertEquals(array1[i], array2[i]);
        }
    }
}
