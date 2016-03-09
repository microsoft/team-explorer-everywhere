// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.jni;

import com.microsoft.tfs.jni.internal.keychain.NativeKeychain;

public class KeychainUtils implements Keychain {
    private static final KeychainUtils instance = new KeychainUtils();

    /**
     * @return an instance of a {@link Keychain} implementation full of utility
     *         methods that are ready-to-call.
     */
    public static KeychainUtils getInstance() {
        return KeychainUtils.instance;
    }

    private final NativeKeychain nativeImpl;

    private KeychainUtils() {
        nativeImpl = new NativeKeychain();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean addInternetPassword(final KeychainInternetPassword password, final boolean allowUi) {
        return nativeImpl.addInternetPassword(password, allowUi);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean modifyInternetPassword(
        final KeychainInternetPassword oldPassword,
        final KeychainInternetPassword newPassword,
        final boolean allowUi) {
        return nativeImpl.modifyInternetPassword(oldPassword, newPassword, allowUi);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public KeychainInternetPassword findInternetPassword(
        final KeychainInternetPassword password,
        final boolean allowUi) {
        return nativeImpl.findInternetPassword(password, allowUi);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean removeInternetPassword(final KeychainInternetPassword password, final boolean allowUi) {
        return nativeImpl.removeInternetPassword(password, allowUi);
    }
}
