// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.jni.internal.console;

import com.microsoft.tfs.jni.Console;

/**
 * An implementation of the {@link Console} interface via external Unix process
 * execution.
 */
public class WindowsExecConsole implements Console {
    @Override
    public boolean disableEcho() {
        return false;
    }

    @Override
    public boolean enableEcho() {
        return false;
    }

    @Override
    public int getConsoleColumns() {
        return 80;
    }

    @Override
    public int getConsoleRows() {
        return 25;
    }
}
