// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.jni;

import com.microsoft.tfs.jni.internal.console.NativeConsole;

public class ConsoleUtils implements Console {
    private static final Console instance = new ConsoleUtils();

    /**
     * @return an instance of a {@link Console} implementation full of utility
     *         methods that are ready-to-call.
     */
    public static Console getInstance() {
        return ConsoleUtils.instance;
    }

    private final NativeConsole nativeImpl;

    private ConsoleUtils() {
        nativeImpl = new NativeConsole();
    }

    @Override
    public int getConsoleColumns() {
        return nativeImpl.getConsoleColumns();
    }

    @Override
    public int getConsoleRows() {
        return nativeImpl.getConsoleRows();
    }

    @Override
    public boolean disableEcho() {
        return nativeImpl.disableEcho();
    }

    @Override
    public boolean enableEcho() {
        return nativeImpl.enableEcho();
    }
}
