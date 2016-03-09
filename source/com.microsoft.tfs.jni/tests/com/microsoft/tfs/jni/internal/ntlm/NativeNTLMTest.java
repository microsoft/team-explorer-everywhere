// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.jni.internal.ntlm;

import com.microsoft.tfs.jni.NTLM.NTLMState;
import com.microsoft.tfs.util.Platform;

import junit.framework.TestCase;

public class NativeNTLMTest extends TestCase {
    private NativeNTLM nativeImpl;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        // Only try to load on Windows
        if (Platform.isCurrentPlatform(Platform.WINDOWS) && NativeNTLM.isAvailable()) {
            nativeImpl = new NativeNTLM();
        }
    }

    public void testNTLMSpecificCredentials() throws Exception {
        if (nativeImpl == null) {
            return;
        }

        NTLMState state;

        state = nativeImpl.initialize();
        assertNotNull("could not initialize NTLM", state); //$NON-NLS-1$

        nativeImpl.setCredentialsSpecified(state, "user", "domain", "pw"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

        final byte[] request = nativeImpl.getToken(state, null);
        assertNotNull("Could not get NTLM request", request); //$NON-NLS-1$
        assertTrue("Could not get NTLM request", request.length > 0); //$NON-NLS-1$

        testNTLMMagic(request);

        nativeImpl.dispose(state);
    }

    public void testNTLMDefaultCredentials() throws Exception {
        if (nativeImpl == null) {
            return;
        }

        NTLMState state;

        state = nativeImpl.initialize();
        assertNotNull("could not initialize NTLM", state); //$NON-NLS-1$

        if (shouldHaveKnownPrincipal()) {
            final String principal = nativeImpl.getCredentialsDefault();
            assertNotNull("could not get default principal", principal); //$NON-NLS-1$
            assertTrue("default principal was empty", principal.length() > 0); //$NON-NLS-1$
        }

        nativeImpl.setCredentialsDefault(state);

        final byte[] request = nativeImpl.getToken(state, null);
        assertNotNull("Could not get NTLM request", request); //$NON-NLS-1$
        assertTrue("Could not get NTLM request", request.length > 0); //$NON-NLS-1$

        testNTLMMagic(request);

        nativeImpl.dispose(state);
    }

    private void testNTLMMagic(final byte[] data) throws Exception {
        if (data[0] != (byte) 'N'
            || data[1] != (byte) 'T'
            || data[2] != (byte) 'L'
            || data[3] != (byte) 'M'
            || data[4] != (byte) 'S'
            || data[5] != (byte) 'S'
            || data[6] != (byte) 'P'
            || data[7] != (byte) 0) {
            throw new Exception("NTLM request is not valid"); //$NON-NLS-1$
        }
    }

    private boolean shouldHaveKnownPrincipal() {
        /* Windows does not know its default principal. */
        return !Platform.isCurrentPlatform(Platform.WINDOWS);
    }
}
