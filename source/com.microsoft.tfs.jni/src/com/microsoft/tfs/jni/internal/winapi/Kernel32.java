package com.microsoft.tfs.jni.internal.winapi;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.WString;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.win32.W32APIOptions;

public interface Kernel32 extends com.sun.jna.platform.win32.Kernel32 {

    Kernel32 INSTANCE = Native.load("Kernel32", Kernel32.class, W32APIOptions.DEFAULT_OPTIONS);
    int ENABLE_ECHO_INPUT = 0x0004;
    int MAX_COMPUTERNAME_LENGTH = 15;

    UINT GetACP();

    boolean GetComputerNameW(Memory lpBuffer, IntByReference nSize);

    DWORD ExpandEnvironmentStringsW(WString lpSrc, Memory lpDst, DWORD nSize);

    boolean SetCurrentDirectoryW(WString lpPathName);

    boolean GetConsoleScreenBufferInfo(HANDLE hConsoleOutput, ConsoleScreenBufferInfo lpConsoleScreenBufferInfo);

    boolean OpenProcessToken(HANDLE ProcessHandle, DWORD DesiredAccess, HANDLEByReference TokenHandle);
}
