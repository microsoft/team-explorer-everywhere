// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.jni.internal.synchronization;

import com.microsoft.tfs.jni.Synchronization;
import com.microsoft.tfs.jni.internal.LibraryNames;
import com.microsoft.tfs.jni.loader.NativeLoader;
import com.microsoft.tfs.util.Check;

public class NativeSynchronization implements Synchronization {
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
        NativeLoader.loadLibraryAndLogError(LibraryNames.SYNCHRONIZATION_LIBRARY_NAME);
    }

    /* Mutexes */

    @Override
    public long createMutex(final String name) {
        Check.notNull(name, "name"); //$NON-NLS-1$

        return nativeCreateMutex(name);
    }

    @Override
    public int waitForMutex(final long mutexId, final int timeout) {
        Check.isTrue(mutexId >= 0, "mutexId >= 0"); //$NON-NLS-1$

        return nativeWaitForMutex(mutexId, timeout);
    }

    @Override
    public boolean releaseMutex(final long mutexId) {
        Check.isTrue(mutexId >= 0, "mutexId >= 0"); //$NON-NLS-1$

        return nativeReleaseMutex(mutexId);
    }

    @Override
    public boolean closeMutex(final long mutexId) {
        Check.isTrue(mutexId >= 0, "mutexId >= 0"); //$NON-NLS-1$

        return nativeCloseMutex(mutexId);
    }

    /*
     * Native method hooks.
     */

    private static native long nativeCreateMutex(String name);

    private static native int nativeWaitForMutex(long mutexId, int timeout);

    private static native boolean nativeReleaseMutex(long mutexId);

    private static native boolean nativeCloseMutex(long mutexId);
}
