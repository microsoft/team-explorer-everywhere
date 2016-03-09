// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.jni.internal.keychain;

import com.microsoft.tfs.jni.Keychain;
import com.microsoft.tfs.jni.KeychainInternetPassword;
import com.microsoft.tfs.jni.internal.LibraryNames;
import com.microsoft.tfs.jni.loader.NativeLoader;
import com.microsoft.tfs.util.Check;

/**
 *
 *
 * @threadsafety unknown
 */
public class NativeKeychain implements Keychain {
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
        NativeLoader.loadLibraryAndLogError(LibraryNames.KEYCHAIN_LIBRARY_NAME);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean addInternetPassword(final KeychainInternetPassword password, final boolean allowUi) {
        Check.notNull(password, "password"); //$NON-NLS-1$

        return nativeAddInternetPassword(password, allowUi);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean modifyInternetPassword(
        final KeychainInternetPassword oldPassword,
        final KeychainInternetPassword newPassword,
        final boolean allowUi) {
        Check.notNull(oldPassword, "oldPassword"); //$NON-NLS-1$
        Check.notNull(newPassword, "newPassword"); //$NON-NLS-1$

        return nativeModifyInternetPassword(oldPassword, newPassword, allowUi);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public KeychainInternetPassword findInternetPassword(
        final KeychainInternetPassword password,
        final boolean allowUi) {
        Check.notNull(password, "password"); //$NON-NLS-1$

        return (KeychainInternetPassword) nativeFindInternetPassword(password, allowUi);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean removeInternetPassword(final KeychainInternetPassword password, final boolean allowUi) {
        Check.notNull(password, "password"); //$NON-NLS-1$

        return nativeRemoveInternetPassword(password, allowUi);
    }

    private static native boolean nativeAddInternetPassword(Object password, boolean allowUi);

    private static native boolean nativeModifyInternetPassword(Object oldPassword, Object newPassword, boolean allowUi);

    private static native Object nativeFindInternetPassword(Object password, boolean allowUi);

    private static native boolean nativeRemoveInternetPassword(Object password, boolean allowUi);
}