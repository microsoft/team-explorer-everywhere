// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.jni.internal.synchronization;

import com.microsoft.tfs.jni.Synchronization;
import com.microsoft.tfs.jni.internal.winapi.Kernel32;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.Platform;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.WinNT;

public class NativeSynchronization implements Synchronization {
    private static final Kernel32 kernel32 = Kernel32.INSTANCE;

    @Override
    public long createMutex(final String name) {
        if (!Platform.isCurrentPlatform(Platform.WINDOWS))
            throw new RuntimeException("Platform not supported");

        Check.notNull(name, "name"); //$NON-NLS-1$

        WinNT.HANDLE mutex = kernel32.CreateMutex(null, false, name);
        if (mutex == null)
            return -1L;

        return Pointer.nativeValue(mutex.getPointer());
    }

    @Override
    public int waitForMutex(final long mutexId, final int timeout) {
        if (!Platform.isCurrentPlatform(Platform.WINDOWS))
            throw new RuntimeException("Platform not supported");

        Check.isTrue(mutexId >= 0, "mutexId >= 0"); //$NON-NLS-1$

        if (mutexId == 0L)
            return -1;

        WinNT.HANDLE mutex = new WinNT.HANDLE(Pointer.createConstant(mutexId));
        int result = kernel32.WaitForSingleObject(mutex, timeout);
        if (result == WinNT.WAIT_ABANDONED || result == WinNT.WAIT_OBJECT_0)
            return 1;

        // Would block: return 0
        if (result == WinNT.WAIT_TIMEOUT)
            return 0;

        // Error: return -1
        return -1;
    }

    @Override
    public boolean releaseMutex(final long mutexId) {
        if (!Platform.isCurrentPlatform(Platform.WINDOWS))
            throw new RuntimeException("Platform not supported");

        Check.isTrue(mutexId >= 0, "mutexId >= 0"); //$NON-NLS-1$

        if (mutexId == 0L)
            return false;

        WinNT.HANDLE mutex = new WinNT.HANDLE(Pointer.createConstant(mutexId));
        return kernel32.ReleaseMutex(mutex);
    }

    @Override
    public boolean closeMutex(final long mutexId) {
        if (!Platform.isCurrentPlatform(Platform.WINDOWS))
            throw new RuntimeException("Platform not supported");

        Check.isTrue(mutexId >= 0, "mutexId >= 0"); //$NON-NLS-1$

        if (mutexId == 0L)
            return false;

        WinNT.HANDLE mutex = new WinNT.HANDLE(Pointer.createConstant(mutexId));
        return kernel32.CloseHandle(mutex);
    }
}
