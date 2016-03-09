// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.jni;

/**
 * Utilities for dealing with Mac OS Keychain.
 *
 * @threadsafety unknown
 */
public interface Keychain {
    /**
     * Adds the given internet password to the (default) keychain.
     *
     * @param password
     *        the internet password to save (not <code>null</code>)
     * @param allowUi
     *        <code>true</code> if the keychain UI may prompt the user for a
     *        keychain password, <code>false</code> otherwise
     * @return <code>true</code> if the keychain password was saved,
     *         <code>false</code> otherwise
     */
    boolean addInternetPassword(final KeychainInternetPassword password, final boolean allowUi);

    /**
     * Modifies the given internet password with the updated values. Works by
     * locating the original password then updating the values, thus the
     * original password is treated as search fields and need not be complete.
     *
     * @param oldPassword
     *        the original internet password to locate (not <code>null</code>)
     * @param newPassword
     *        the new internet password values (not <code>null</code>)
     * @param allowUi
     *        <code>true</code> if the keychain UI may prompt the user for a
     *        keychain password, <code>false</code> otherwise
     * @return <code>true</code> if the keychain password was modified,
     *         <code>false</code> otherwise
     */
    boolean modifyInternetPassword(
        final KeychainInternetPassword oldPassword,
        final KeychainInternetPassword newPassword,
        final boolean allowUi);

    /**
     * Locates the given internet password, the value provided is treated as
     * search fields and need not be complete.
     *
     * @param password
     *        the internet password to locate (not <code>null</code>)
     * @param allowUi
     *        <code>true</code> if the keychain UI may prompt the user for a
     *        keychain password, <code>false</code> otherwise
     * @return the located internet password, or <code>null</code> if it could
     *         not be located
     */
    KeychainInternetPassword findInternetPassword(final KeychainInternetPassword password, boolean allowUi);

    /**
     * Removes the given internet password from the keychain. The given password
     * is treated as search fields and need not be complete, however only the
     * first match will be deleted.
     *
     * @param password
     *        the internet password to remove (not <code>null</code>)
     * @param allowUi
     *        <code>true</code> if the keychain UI may prompt the user for a
     *        keychain password, <code>false</code> otherwise
     * @return <code>true</code> if the keychain password was saved,
     *         <code>false</code> otherwise
     */
    boolean removeInternetPassword(final KeychainInternetPassword password, boolean allowUi);
}
