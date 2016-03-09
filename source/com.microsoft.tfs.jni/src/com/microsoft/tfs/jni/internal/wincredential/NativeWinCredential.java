// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.jni.internal.wincredential;

import com.microsoft.tfs.jni.WinCredential;
import com.microsoft.tfs.jni.internal.LibraryNames;
import com.microsoft.tfs.jni.loader.NativeLoader;

public class NativeWinCredential {
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
        NativeLoader.loadLibraryAndLogError(LibraryNames.WINDOWS_CREDENTIAL_LIBRARY_NAME);
    }

    public boolean storeCredential(final WinCredential credential) {
        return nativeStoreCredential(credential.getServerUri(), credential.getAccountName(), credential.getPassword());
    }

    public WinCredential findCredential(final WinCredential credential) {
        final Object winCredential = nativeFindCredential(credential.getServerUri());
        if (winCredential == null) {
            return null;
        } else {
            return (WinCredential) winCredential;
        }
    }

    public boolean eraseCredential(final WinCredential cred) {
        return nativeEraseCredential(cred.getServerUri());
    }

    private static native boolean nativeStoreCredential(String uri, String accountName, String password);

    private static native Object nativeFindCredential(String serverUri);

    private static native boolean nativeEraseCredential(String uri);
}