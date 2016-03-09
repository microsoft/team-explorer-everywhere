// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.jni.internal.console;

import java.text.MessageFormat;

import com.microsoft.tfs.jni.Console;
import com.microsoft.tfs.util.Platform;

/**
 * A {@link Console} implemented with external processes.
 */
public class ExecConsole implements Console {
    private final Console delegate;

    public ExecConsole() {
        if (Platform.isCurrentPlatform(Platform.WINDOWS)) {
            delegate = new WindowsExecConsole();
        } else if (Platform.isCurrentPlatform(Platform.GENERIC_UNIX)) {
            delegate = new UnixExecConsole();
        } else {
            throw new RuntimeException(
                MessageFormat.format(
                    "There is no ExecPlatformUtils functionality available for this platform ({0})", //$NON-NLS-1$
                    Platform.getCurrentPlatformString()));
        }
    }

    @Override
    public int getConsoleColumns() {
        return delegate.getConsoleColumns();
    }

    @Override
    public int getConsoleRows() {
        return delegate.getConsoleRows();
    }

    @Override
    public boolean disableEcho() {
        return delegate.disableEcho();
    }

    @Override
    public boolean enableEcho() {
        return delegate.enableEcho();
    }
}
