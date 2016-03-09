// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.jni.internal.auth;

import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.jni.internal.LibraryNames;
import com.microsoft.tfs.jni.internal.negotiate.NativeNegotiate;
import com.microsoft.tfs.jni.internal.ntlm.NativeNTLM;
import com.microsoft.tfs.jni.loader.NativeLoader;
import com.microsoft.tfs.util.Check;

/**
 * Wraps the low-level authentication native library that uses GSSAPI/Kerberos
 * on Windows and Unix platforms. Not very friendly when used directly; use
 * {@link NativeNegotiate} or {@link NativeNTLM} instead.
 *
 * @threadsafety thread-safe
 */
public abstract class NativeAuth {
    public static final short MECHANISM_NTLM = 1;
    public static final short MECHANISM_NEGOTIATE = 2;

    /**
     * Initialized during the static load and used by non-static methods.
     *
     * TODO Ensure this is only assigned once per process (not per-classloader).
     * Probably requires native tricks for global state.
     */
    private static long authConfiguration = 0;

    /**
     * This static initializer is a "best-effort" native code loader (no
     * exceptions thrown for normal load failures).
     *
     * Apps with multiple classloaders (like Eclipse) can run this initializer
     * more than once in a single JVM OS process, and on some platforms
     * (Windows) the native libraries will fail to load the second time, because
     * they're already loaded. This failure can be ignored because the native
     * code will execute fine.
     */
    static {
        NativeLoader.loadLibraryAndLogError(LibraryNames.AUTH_LIBRARY_NAME);

        try {
            // Assigns 0 on error
            authConfiguration = nativeAuthConfigure();
        } catch (final Throwable t) {
            LogFactory.getLog(NativeAuth.class).error("Error configuring native authentication library", t); //$NON-NLS-1$
        }
    }

    // This class is just for static methods.
    private NativeAuth() {
    }

    public static boolean authAvailable(final short mechanism) {
        return nativeAuthAvailable(authConfiguration, mechanism);
    }

    public static boolean authSupportsCredentialsDefault(final short mechanism) {
        return nativeAuthSupportsCredentialsDefault(authConfiguration, mechanism);
    }

    public static boolean authSupportsCredentialsSpecified(final short mechanism) {
        return nativeAuthSupportsCredentialsSpecified(authConfiguration, mechanism);
    }

    public static String authGetCredentialsDefault(final short mechanism) {
        return nativeAuthGetCredentialsDefault(authConfiguration, mechanism);
    }

    public static long authInitialize(final short mechanism) {
        return nativeAuthInitialize(authConfiguration, mechanism);
    }

    public static void authSetCredentialsDefault(final long authId) {
        Check.isTrue(authId != 0, "authId != 0"); //$NON-NLS-1$

        nativeAuthSetCredentialsDefault(authId);
    }

    public static void authSetCredentialsSpecified(
        final long authId,
        final String username,
        final String domain,
        final String password) {
        Check.isTrue(authId != 0, "authId != 0"); //$NON-NLS-1$
        Check.notNull(username, "username"); //$NON-NLS-1$
        Check.notNull(domain, "domain"); //$NON-NLS-1$
        Check.notNull(password, "password"); //$NON-NLS-1$

        nativeAuthSetCredentialsSpecified(authId, username, domain, password);
    }

    public static void authSetTarget(final long authId, final String target) {
        Check.isTrue(authId != 0, "authId != 0"); //$NON-NLS-1$
        Check.notNull(target, "target"); //$NON-NLS-1$

        nativeAuthSetTarget(authId, target);
    }

    public static void authSetLocalhost(final long authId, final String localhost) {
        Check.isTrue(authId != 0, "authId != 0"); //$NON-NLS-1$
        Check.notNull(localhost, "localhost"); //$NON-NLS-1$

        nativeAuthSetLocalhost(authId, localhost);
    }

    public static byte[] authGetToken(final long authId, final byte[] inputToken) {
        Check.isTrue(authId != 0, "authId != 0"); //$NON-NLS-1$

        return nativeAuthGetToken(authId, inputToken);
    }

    public static boolean authIsComplete(final long authId) {
        Check.isTrue(authId != 0, "authId != 0"); //$NON-NLS-1$

        return nativeAuthIsComplete(authId);
    }

    public static String authGetErrorMessage(final long authId) {
        Check.isTrue(authId != 0, "authId != 0"); //$NON-NLS-1$

        return nativeAuthGetErrorMessage(authId);
    }

    public static void authDispose(final long authId) {
        Check.isTrue(authId != 0, "authId != 0"); //$NON-NLS-1$

        nativeAuthDispose(authId);
    }

    private static native long nativeAuthConfigure();

    private static native boolean nativeAuthAvailable(long configurationId, short mechanism);

    private static native boolean nativeAuthSupportsCredentialsDefault(long configurationId, short mechanism);

    private static native boolean nativeAuthSupportsCredentialsSpecified(long configurationId, short mechanism);

    private static native String nativeAuthGetCredentialsDefault(long configurationId, short mechanism);

    private static native long nativeAuthInitialize(long configurationId, short mechanism);

    private static native void nativeAuthSetTarget(long negotiateId, String target);

    private static native void nativeAuthSetLocalhost(long negotiateId, String localhost);

    private static native void nativeAuthSetCredentialsDefault(long negotiateId);

    private static native void nativeAuthSetCredentialsSpecified(
        long negotiateId,
        String username,
        String domain,
        String password);

    private static native byte[] nativeAuthGetToken(long negotiateId, byte[] inputToken);

    private static native boolean nativeAuthIsComplete(long negotiateId);

    private static native String nativeAuthGetErrorMessage(long negotiateId);

    private static native void nativeAuthDispose(long negotiateId);
}