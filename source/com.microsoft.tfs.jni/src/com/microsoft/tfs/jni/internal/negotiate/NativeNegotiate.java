// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.jni.internal.negotiate;

import java.text.MessageFormat;

import com.microsoft.tfs.jni.Messages;
import com.microsoft.tfs.jni.Negotiate;
import com.microsoft.tfs.jni.internal.auth.NativeAuth;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.Platform;

/**
 * An implementation of the {@link Negotiate} interface that uses native
 * methods.
 *
 * @threadsafety thread-safe
 */
public class NativeNegotiate implements Negotiate {
    /**
     * @return <code>true</code> if native Negotiate is available on this sytem,
     *         <code>false</code> if not
     */
    public static boolean isAvailable() {
        /*
         * This is an ugly test for a simple work-around. HP's Kerberos on HP-UX
         * 11.11 on PA_RISC usually crashes when first Kerberos methods are
         * called. Probably a thread-safety issue because it only happens in the
         * Eclipse client, not in the CLC. This architecture is nearly dead, so
         * disable the native library there when Eclipse is running.
         */
        if (Platform.isCurrentPlatform(Platform.HPUX)
            && "PA_RISC2.0".equals(System.getProperty("os.arch")) //$NON-NLS-1$//$NON-NLS-2$
            && System.getProperty("eclipse.product") != null) //$NON-NLS-1$
        {
            return false;
        }

        /*
         * We can't reliably detect whether a native library is already loaded
         * into the process (because classloaders may force more than one load
         * attempt and those mail fail even though the library is loaded and can
         * be used). So just handle the common exceptions as "not available."
         */
        try {
            return NativeAuth.authAvailable(NativeAuth.MECHANISM_NEGOTIATE);
        } catch (final UnsatisfiedLinkError e) {
            return false;
        } catch (final LinkageError e) {
            return false;
        }
    }

    public NativeNegotiate() {
    }

    @Override
    public synchronized boolean supportsCredentialsDefault() {
        return NativeAuth.authSupportsCredentialsDefault(NativeAuth.MECHANISM_NEGOTIATE);
    }

    @Override
    public synchronized boolean supportsCredentialsSpecified() {
        return NativeAuth.authSupportsCredentialsSpecified(NativeAuth.MECHANISM_NEGOTIATE);
    }

    @Override
    public synchronized String getCredentialsDefault() {
        return NativeAuth.authGetCredentialsDefault(NativeAuth.MECHANISM_NEGOTIATE);
    }

    @Override
    public synchronized NegotiateState initialize() throws NegotiateException {
        try {
            final long id = NativeAuth.authInitialize(NativeAuth.MECHANISM_NEGOTIATE);

            if (id == 0) {
                throw new NegotiateException(
                    Messages.getString("NativeNegotiate.CouldNotInitializeNegotiateLibraries")); //$NON-NLS-1$
            }

            return new NativeNegotiateState(id);
        } catch (final Exception e) {
            throw new NegotiateException(e.getMessage());
        }
    }

    @Override
    public synchronized void setCredentialsDefault(final NegotiateState state) throws NegotiateException {
        Check.notNull(state, "state"); //$NON-NLS-1$
        Check.isTrue(state instanceof NativeNegotiateState, "state instanceof NativeNegotiateState"); //$NON-NLS-1$

        try {
            NativeAuth.authSetCredentialsDefault(((NativeNegotiateState) state).id);
        } catch (final Exception e) {
            throw new NegotiateException(e.getMessage());
        }
    }

    @Override
    public synchronized void setCredentialsSpecified(
        final NegotiateState state,
        final String username,
        final String domain,
        final String password) throws NegotiateException {
        Check.notNull(state, "state"); //$NON-NLS-1$
        Check.isTrue(state instanceof NativeNegotiateState, "state instanceof NativeNegotiateState"); //$NON-NLS-1$

        try {
            NativeAuth.authSetCredentialsSpecified(((NativeNegotiateState) state).id, username, domain, password);
        } catch (final Exception e) {
            throw new NegotiateException(e.getMessage());
        }
    }

    @Override
    public synchronized void setLocalhost(final NegotiateState state, final String localhost)
        throws NegotiateException {
        Check.notNull(state, "state"); //$NON-NLS-1$
        Check.isTrue(state instanceof NativeNegotiateState, "state instanceof NativeNegotiateState"); //$NON-NLS-1$

        try {
            NativeAuth.authSetLocalhost(((NativeNegotiateState) state).id, (localhost != null ? localhost : "")); //$NON-NLS-1$
        } catch (final Exception e) {
            throw new NegotiateException(e.getMessage());
        }
    }

    @Override
    public synchronized void setTarget(final NegotiateState state, final String target) throws NegotiateException {
        Check.notNull(state, "state"); //$NON-NLS-1$
        Check.isTrue(state instanceof NativeNegotiateState, "state instanceof NativeNegotiateState"); //$NON-NLS-1$

        try {
            NativeAuth.authSetTarget(((NativeNegotiateState) state).id, (target != null ? target : "")); //$NON-NLS-1$
        } catch (final Exception e) {
            throw new NegotiateException(e.getMessage());
        }
    }

    @Override
    public synchronized byte[] getToken(final NegotiateState state, final byte[] inputToken) throws NegotiateException {
        Check.notNull(state, "state"); //$NON-NLS-1$
        Check.isTrue(state instanceof NativeNegotiateState, "state instanceof NativeNegotiateState"); //$NON-NLS-1$

        try {
            return NativeAuth.authGetToken(((NativeNegotiateState) state).id, inputToken);
        } catch (final Exception e) {
            throw new NegotiateException(e.getMessage());
        }
    }

    @Override
    public synchronized boolean isComplete(final NegotiateState state) throws NegotiateException {
        Check.notNull(state, "state"); //$NON-NLS-1$
        Check.isTrue(state instanceof NativeNegotiateState, "state instanceof NativeNegotiateState"); //$NON-NLS-1$

        try {
            return NativeAuth.authIsComplete(((NativeNegotiateState) state).id);
        } catch (final Exception e) {
            throw new NegotiateException(e.getMessage());
        }
    }

    @Override
    public synchronized String getErrorMessage(final NegotiateState state) {
        Check.notNull(state, "state"); //$NON-NLS-1$

        try {
            return NativeAuth.authGetErrorMessage(((NativeNegotiateState) state).id);
        } catch (final Exception e) {
            return MessageFormat.format(
                Messages.getString("NativeNegotiate.CouldNotCallNativeNegotiateFormat"), //$NON-NLS-1$
                e.getLocalizedMessage());
        }
    }

    @Override
    public synchronized void dispose(final NegotiateState state) throws NegotiateException {
        Check.notNull(state, "state"); //$NON-NLS-1$
        Check.isTrue(state instanceof NativeNegotiateState, "state instanceof NativeNegotiateState"); //$NON-NLS-1$

        try {
            NativeAuth.authDispose(((NativeNegotiateState) state).id);
        } catch (final Exception e) {
            throw new NegotiateException(e.getMessage());
        }
    }

    private static class NativeNegotiateState extends NegotiateState {
        /* The id (a pointer to the native NTLM struct */
        private long id = 0;

        public NativeNegotiateState(final long id) {
            this.id = id;
        }
    }
}