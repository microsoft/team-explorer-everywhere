// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.jni.internal.negotiate;

import com.microsoft.tfs.jni.Negotiate.NegotiateState;
import com.microsoft.tfs.util.Platform;

import junit.framework.TestCase;

public class NativeNegotiateTest extends TestCase {
    private NativeNegotiate nativeImpl;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        // All these platforms have Negotiate support
        if ((Platform.isCurrentPlatform(Platform.WINDOWS)
            || Platform.isCurrentPlatform(Platform.LINUX)
            || Platform.isCurrentPlatform(Platform.AIX)
            || Platform.isCurrentPlatform(Platform.SOLARIS)
            || Platform.isCurrentPlatform(Platform.HPUX)
            || Platform.isCurrentPlatform(Platform.MAC_OS_X)) && NativeNegotiate.isAvailable()) {
            nativeImpl = new NativeNegotiate();
        }
    }

    public void testNegotiateDefaultCredentials() throws Exception {
        if (nativeImpl == null) {
            return;
        }

        NegotiateState state;

        state = nativeImpl.initialize();
        assertNotNull("could not initialize negotiate", state); //$NON-NLS-1$

        if (shouldHaveKnownPrincipal()) {
            final String principal = nativeImpl.getCredentialsDefault();
            assertNotNull("could not get default principal", principal); //$NON-NLS-1$
            assertTrue("default principal was empty", principal.length() > 0); //$NON-NLS-1$
        }

        nativeImpl.dispose(state);
    }

    public void testNegotiateCallAllMethods() throws Exception {
        if (nativeImpl == null) {
            return;
        }

        /*
         * Calls all methods to ensure they don't crash. Doesn't actually test
         * that they behave correctly.
         */

        nativeImpl.supportsCredentialsDefault();
        nativeImpl.supportsCredentialsSpecified();
        nativeImpl.getCredentialsDefault();

        final NegotiateState state = nativeImpl.initialize();

        nativeImpl.setCredentialsSpecified(state, "username", "domain", "password"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        nativeImpl.setCredentialsDefault(state);
        System.out.println("Trying some Kerberos auth to a bogus server, this could take a minute to time out..."); //$NON-NLS-1$
        nativeImpl.setTarget(state, "server.not.existing.com"); //$NON-NLS-1$
        nativeImpl.setLocalhost(state, "localhost.domain.name"); //$NON-NLS-1$
        nativeImpl.getToken(state, new byte[] {
            0x00,
            0x00,
            0x00,
            0x00,
            0x00,
            0x00
        });
        nativeImpl.isComplete(state);
        nativeImpl.getErrorMessage(state);

        nativeImpl.dispose(state);
    }

    private boolean shouldHaveKnownPrincipal() {
        /* Windows does not know its default principal. */
        return !Platform.isCurrentPlatform(Platform.WINDOWS);
    }
}
