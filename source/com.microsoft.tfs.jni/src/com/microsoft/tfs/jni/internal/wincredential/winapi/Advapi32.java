package com.microsoft.tfs.jni.internal.wincredential.winapi;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.WString;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinReg;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;
import com.sun.jna.win32.W32APIOptions;

public interface Advapi32 extends com.sun.jna.platform.win32.Advapi32 {

    Advapi32 INSTANCE = Native.load("Advapi32", Advapi32.class, W32APIOptions.DEFAULT_OPTIONS);

    int CRED_TYPE_GENERIC = 1;
    int CRED_PERSIST_LOCAL_MACHINE = 2;

    boolean CredReadW(WString TargetName, WinDef.DWORD Type, WinDef.DWORD Flags, PointerByReference Credential);
    boolean CredWriteW(CREDENTIALW Credential, WinDef.DWORD Flags);
    boolean CredDeleteW(WString TargetName, WinDef.DWORD Type, WinDef.DWORD Flags);
    void CredFree(Pointer Buffer);

    int RegQueryValueExW(
        WinReg.HKEY hkey,
        String lpValueName,
        IntByReference lpReserved,
        IntByReference lpType,
        byte[] lpData,
        IntByReference lpcbData);
}
