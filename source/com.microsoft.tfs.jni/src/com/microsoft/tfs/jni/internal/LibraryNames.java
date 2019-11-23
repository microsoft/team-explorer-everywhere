// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.jni.internal;

/**
 * Static identifiers for native libraries.
 */
public abstract class LibraryNames {
    /**
     * The short name of the native filesystem library.
     */
    public static final String FILESYSTEM_LIBRARY_NAME = "native_filesystem"; //$NON-NLS-1$

    /**
     * The short name of the authentication library.
     */
    public static final String AUTH_LIBRARY_NAME = "native_auth"; //$NON-NLS-1$

    /**
     * The short name of the keychain library.
     */
    public static final String KEYCHAIN_LIBRARY_NAME = "native_keychain"; //$NON-NLS-1$

    /**
     * The short name of the synchronization (mutex and semaphore) library.
     */
    public static final String SYNCHRONIZATION_LIBRARY_NAME = "native_synchronization"; //$NON-NLS-1$

    /**
     * The short name of the Windows credential library.
     */
    public static final String WINDOWS_CREDENTIAL_LIBRARY_NAME = "native_credential"; //$NON-NLS-1$
    /**
     * The short name of the Windows message window library.
     */
    public static final String WINDOWS_MESSAGEWINDOW_LIBRARY_NAME = "native_messagewindow"; //$NON-NLS-1$
}
