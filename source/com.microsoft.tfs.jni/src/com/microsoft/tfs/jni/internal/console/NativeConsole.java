// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.jni.internal.console;

import com.microsoft.tfs.jni.Console;
import com.microsoft.tfs.util.Platform;

/**
 * An implementation of the {@link Console} interface that uses native methods.
 *
 * @threadsafety thread-safe
 */
public class NativeConsole implements Console {

    private static final Console backend = Platform.isCurrentPlatform(Platform.WINDOWS)
        ? new WindowsNativeConsole()
        : new UnixNativeConsole(
            Platform.isCurrentPlatform(Platform.MAC_OS_X)
                ? com.microsoft.tfs.jni.internal.unix.macos.LibC.INSTANCE
                : com.microsoft.tfs.jni.internal.unix.linux.LibC.INSTANCE);

    public NativeConsole() {
    }

    @Override
    public int getConsoleColumns() {
        return backend.getConsoleColumns();
    }

    @Override
    public int getConsoleRows() {
        return backend.getConsoleRows();
    }

    @Override
    public boolean disableEcho() {
        return backend.disableEcho();
    }

    @Override
    public boolean enableEcho() {
        return backend.enableEcho();
    }
}
