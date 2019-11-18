// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.jni.internal.console;

import com.microsoft.tfs.jni.Console;
import com.microsoft.tfs.jni.internal.LibraryNames;
import com.microsoft.tfs.jni.loader.NativeLoader;
import com.microsoft.tfs.util.Platform;

/**
 * An implementation of the {@link Console} interface that uses native methods.
 *
 * @threadsafety thread-safe
 */
public class NativeConsole implements Console {

    private static Console backend;

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
        if (Platform.isCurrentPlatform(Platform.WINDOWS)) {
            backend = new WindowsNativeConsole();
        } else {
            NativeLoader.loadLibraryAndLogError(LibraryNames.CONSOLE_LIBRARY_NAME);
        }
    }

    public NativeConsole() {
    }

    @Override
    public int getConsoleColumns() {
        return backend == null ? nativeGetColumns() : backend.getConsoleColumns();
    }

    @Override
    public int getConsoleRows() {
        return backend == null ?  nativeGetRows() : backend.getConsoleRows();
    }

    @Override
    public boolean disableEcho() {
        return backend == null ? nativeDisableEcho() : backend.disableEcho();
    }

    @Override
    public boolean enableEcho() {
        return backend == null ? nativeEnableEcho() : backend.enableEcho();
    }

    private static native int nativeGetRows();

    private static native int nativeGetColumns();

    private static native boolean nativeDisableEcho();

    private static native boolean nativeEnableEcho();
}
