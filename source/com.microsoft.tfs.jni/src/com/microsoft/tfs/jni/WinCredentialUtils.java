// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.jni;

import com.microsoft.tfs.jni.internal.wincredential.NativeWinCredential;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.StringUtil;

public class WinCredentialUtils {
    private static final WinCredentialUtils instance = new WinCredentialUtils();

    private final NativeWinCredential nativeImpl;

    public static WinCredentialUtils getInstance() {
        return WinCredentialUtils.instance;
    }

    private WinCredentialUtils() {
        nativeImpl = new NativeWinCredential();
    }

    public boolean storeCredential(final WinCredential credential) {
        Check.notNull(credential, "credential"); //$NON-NLS-1$

        return nativeImpl.storeCredential(credential);
    }

    public WinCredential findCredential(final WinCredential credential) {
        Check.notNull(credential, "credential"); //$NON-NLS-1$

        if (StringUtil.isNullOrEmpty(credential.getServerUri())) {
            return null;
        }

        final Object winCredential = nativeImpl.findCredential(credential);
        if (winCredential == null) {
            return null;
        } else {
            return (WinCredential) winCredential;
        }
    }

    public boolean eraseCredential(final WinCredential credential) {
        Check.notNull(credential, "credential"); //$NON-NLS-1$

        return nativeImpl.eraseCredential(credential);
    }
}