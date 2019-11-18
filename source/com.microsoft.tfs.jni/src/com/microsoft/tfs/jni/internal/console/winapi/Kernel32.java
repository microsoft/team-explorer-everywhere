package com.microsoft.tfs.jni.internal.console.winapi;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

public interface Kernel32 extends Library {

    Kernel32 INSTANCE = (Kernel32) Native.loadLibrary("Kernel32", Kernel32.class);
    int STD_OUTPUT_HANDLE = -11;
    int ENABLE_ECHO_INPUT = 0x0004;

    Pointer GetStdHandle(int nStdHandle);

    boolean GetConsoleScreenBufferInfo(Pointer hConsoleOutput, ConsoleScreenBufferInfo lpConsoleScreenBufferInfo);

    boolean GetConsoleMode(Pointer hConsoleHandle, IntByReference lpMode);

    boolean SetConsoleMode(Pointer hConsoleHandle, int lpMode);
}
