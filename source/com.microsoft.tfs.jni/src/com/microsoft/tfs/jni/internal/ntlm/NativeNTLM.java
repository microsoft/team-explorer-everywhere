// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.jni.internal.ntlm;

import java.text.MessageFormat;

import com.microsoft.tfs.jni.FileSystem;
import com.microsoft.tfs.jni.Messages;
import com.microsoft.tfs.jni.NTLM;
import com.microsoft.tfs.jni.internal.auth.NativeAuth;
import com.microsoft.tfs.util.Check;

/**
 * An implementation of the {@link FileSystem} interface that uses native
 * methods.
 *
 * @threadsafety thread-safe
 */
public class NativeNTLM implements NTLM {
    /**
     * @return <code>true</code> if native NTLM is available on this sytem,
     *         <code>false</code> if not
     */
    public static boolean isAvailable() {
        /*
         * We can't reliably detect whether a native library is already loaded
         * into the process (because classloaders may force more than one load
         * attempt and those mail fail even though the library is loaded and can
         * be used). So just handle the common exceptions as "not available."
         */
        try {
            return NativeAuth.authAvailable(NativeAuth.MECHANISM_NTLM);
        } catch (final UnsatisfiedLinkError e) {
            return false;
        } catch (final LinkageError e) {
            return false;
        }
    }

    public NativeNTLM() {
    }

    @Override
    public boolean supportsCredentialsDefault() {
        return NativeAuth.authSupportsCredentialsDefault(NativeAuth.MECHANISM_NTLM);
    }

    @Override
    public boolean supportsCredentialsSpecified() {
        return NativeAuth.authSupportsCredentialsSpecified(NativeAuth.MECHANISM_NTLM);
    }

    @Override
    public String getCredentialsDefault() {
        return NativeAuth.authGetCredentialsDefault(NativeAuth.MECHANISM_NTLM);
    }

    @Override
    public NTLMState initialize() throws NTLMException {
        try {
            final long id = NativeAuth.authInitialize(NativeAuth.MECHANISM_NTLM);

            if (id == 0) {
                throw new NTLMException(Messages.getString("NativeNTLM.CouldNotInitializeNTLMLibraries")); //$NON-NLS-1$
            }

            return new NativeNTLMState(id);
        } catch (final Exception e) {
            throw new NTLMException(e.getMessage());
        }
    }

    @Override
    public void setCredentialsDefault(final NTLMState state) throws NTLMException {
        Check.notNull(state, "state"); //$NON-NLS-1$
        Check.isTrue(state instanceof NativeNTLMState, "state instanceof NativeNTLMState"); //$NON-NLS-1$

        try {
            NativeAuth.authSetCredentialsDefault(((NativeNTLMState) state).id);
        } catch (final Exception e) {
            throw new NTLMException(e.getMessage());
        }
    }

    @Override
    public void setCredentialsSpecified(
        final NTLMState state,
        final String username,
        final String domain,
        final String password) throws NTLMException {
        Check.notNull(state, "state"); //$NON-NLS-1$
        Check.isTrue(state instanceof NativeNTLMState, "state instanceof NativeNTLMState"); //$NON-NLS-1$

        try {
            NativeAuth.authSetCredentialsSpecified(((NativeNTLMState) state).id, username, domain, password);
        } catch (final Exception e) {
            throw new NTLMException(e.getMessage());
        }
    }

    @Override
    public void setLocalhost(final NTLMState state, final String localhost) throws NTLMException {
        Check.notNull(state, "state"); //$NON-NLS-1$
        Check.isTrue(state instanceof NativeNTLMState, "state instanceof NativeNTLMState"); //$NON-NLS-1$

        try {
            NativeAuth.authSetLocalhost(((NativeNTLMState) state).id, (localhost != null ? localhost : "")); //$NON-NLS-1$
        } catch (final Exception e) {
            throw new NTLMException(e.getMessage());
        }
    }

    @Override
    public void setTarget(final NTLMState state, final String target) throws NTLMException {
        Check.notNull(state, "state"); //$NON-NLS-1$
        Check.isTrue(state instanceof NativeNTLMState, "state instanceof NativeNTLMState"); //$NON-NLS-1$

        try {
            NativeAuth.authSetTarget(((NativeNTLMState) state).id, (target != null ? target : "")); //$NON-NLS-1$
        } catch (final Exception e) {
            throw new NTLMException(e.getMessage());
        }
    }

    @Override
    public byte[] getToken(final NTLMState state, final byte[] inputToken) throws NTLMException {
        Check.notNull(state, "state"); //$NON-NLS-1$
        Check.isTrue(state instanceof NativeNTLMState, "state instanceof NativeNTLMState"); //$NON-NLS-1$

        try {
            return NativeAuth.authGetToken(((NativeNTLMState) state).id, inputToken);
        } catch (final Exception e) {
            throw new NTLMException(e.getMessage());
        }
    }

    @Override
    public boolean isComplete(final NTLMState state) throws NTLMException {
        Check.notNull(state, "state"); //$NON-NLS-1$
        Check.isTrue(state instanceof NativeNTLMState, "state instanceof NativeNTLMState"); //$NON-NLS-1$

        try {
            return NativeAuth.authIsComplete(((NativeNTLMState) state).id);
        } catch (final Exception e) {
            throw new NTLMException(e.getMessage());
        }
    }

    @Override
    public String getErrorMessage(final NTLMState state) {
        Check.notNull(state, "state"); //$NON-NLS-1$
        Check.isTrue(state instanceof NativeNTLMState, "state instanceof NativeNTLMState"); //$NON-NLS-1$

        try {
            return NativeAuth.authGetErrorMessage(((NativeNTLMState) state).id);
        } catch (final Exception e) {
            return MessageFormat.format(
                Messages.getString("NativeNTLM.CouldNotCallNativeNTLMFormat"), //$NON-NLS-1$
                e.getLocalizedMessage());
        }
    }

    @Override
    public void dispose(final NTLMState state) throws NTLMException {
        Check.notNull(state, "state"); //$NON-NLS-1$
        Check.isTrue(state instanceof NativeNTLMState, "state instanceof NativeNTLMState"); //$NON-NLS-1$

        try {
            NativeAuth.authDispose(((NativeNTLMState) state).id);
        } catch (final Exception e) {
            throw new NTLMException(e.getMessage());
        }
    }

    private static class NativeNTLMState extends NTLMState {
        /* The id (a pointer to the native NTLM struct */
        private long id = 0;

        public NativeNTLMState(final long id) {
            this.id = id;
        }
    }
}
