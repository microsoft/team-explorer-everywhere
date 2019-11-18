package com.microsoft.tfs.jni.internal.console.winapi;

import com.sun.jna.Native;

public interface Kernel32 extends com.sun.jna.platform.win32.Kernel32 {

    Kernel32 INSTANCE = (Kernel32) Native.loadLibrary("Kernel32", Kernel32.class);
    int ENABLE_ECHO_INPUT = 0x0004;

    boolean GetConsoleScreenBufferInfo(HANDLE hConsoleOutput, ConsoleScreenBufferInfo lpConsoleScreenBufferInfo);
}
