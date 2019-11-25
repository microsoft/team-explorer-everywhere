// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.jni.internal.keychain;

import com.microsoft.tfs.jni.Keychain;
import com.microsoft.tfs.jni.KeychainInternetPassword;
import com.microsoft.tfs.util.Check;

/**
 *
 *
 * @threadsafety unknown
 */
public class NativeKeychain implements Keychain {

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

    private static boolean nativeAddInternetPassword(Object password, boolean allowUi) {
        return false;
    }

    private static boolean nativeModifyInternetPassword(Object oldPassword, Object newPassword, boolean allowUi) {
        return false;
    }

    private static Object nativeFindInternetPassword(Object password, boolean allowUi) {
        return null;
    }

    private static boolean nativeRemoveInternetPassword(Object password, boolean allowUi) {
        return false;
    }
}