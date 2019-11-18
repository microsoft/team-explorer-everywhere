package com.microsoft.tfs.jni.internal.console;

import com.microsoft.tfs.jni.Console;
import com.microsoft.tfs.jni.internal.console.winapi.ConsoleScreenBufferInfo;
import com.microsoft.tfs.jni.internal.console.winapi.Kernel32;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.ptr.IntByReference;

import static com.microsoft.tfs.jni.internal.console.winapi.Kernel32.ENABLE_ECHO_INPUT;

class WindowsNativeConsole implements Console {
    private static ConsoleScreenBufferInfo getConsoleScreenBufferInfo() {
        Kernel32 kernel32 = Kernel32.INSTANCE;
        WinNT.HANDLE handle = kernel32.GetStdHandle(Kernel32.STD_OUTPUT_HANDLE);
        if (handle.getPointer().equals(Pointer.NULL))
            return null;

        ConsoleScreenBufferInfo info = new ConsoleScreenBufferInfo();
        if (!kernel32.GetConsoleScreenBufferInfo(handle, info))
            return null;

        return info;
    }

    @Override public int getConsoleColumns() {
        ConsoleScreenBufferInfo info = getConsoleScreenBufferInfo();
        return info == null ? 0 : info.dwSize.x;
    }

    @Override public int getConsoleRows() {
        ConsoleScreenBufferInfo info = getConsoleScreenBufferInfo();
        return info == null ? 0 : info.dwSize.y;
    }

    @Override public boolean disableEcho() {
        Kernel32 kernel32 = Kernel32.INSTANCE;
        WinNT.HANDLE stdOut = kernel32.GetStdHandle(Kernel32.STD_OUTPUT_HANDLE);
        if (stdOut.equals(Pointer.NULL))
            return false;

        IntByReference consoleMode = new IntByReference();
        if (!kernel32.GetConsoleMode(stdOut, consoleMode))
            return false;

        int newConsoleMode = consoleMode.getValue() & ~ENABLE_ECHO_INPUT;
        return kernel32.SetConsoleMode(stdOut, newConsoleMode);
    }

    @Override public boolean enableEcho() {
        Kernel32 kernel32 = Kernel32.INSTANCE;
        WinNT.HANDLE stdOut = kernel32.GetStdHandle(Kernel32.STD_OUTPUT_HANDLE);
        if (stdOut.equals(Pointer.NULL))
            return false;

        IntByReference consoleMode = new IntByReference();
        if (!kernel32.GetConsoleMode(stdOut, consoleMode))
            return false;

        int newConsoleMode = consoleMode.getValue() | ENABLE_ECHO_INPUT;
        return kernel32.SetConsoleMode(stdOut, newConsoleMode);
    }
}
