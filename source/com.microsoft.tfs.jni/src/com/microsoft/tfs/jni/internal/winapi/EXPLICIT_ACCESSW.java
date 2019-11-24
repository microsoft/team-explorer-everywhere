package com.microsoft.tfs.jni.internal.winapi;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.platform.win32.WinDef;

import java.util.Arrays;
import java.util.List;

public class EXPLICIT_ACCESSW extends Structure {
    public WinDef.DWORD grfAccessPermissions;
    public int grfAccessMode;
    public WinDef.DWORD grfInheritance;
    public TRUSTEEW Trustee;

    public EXPLICIT_ACCESSW() {}

    public EXPLICIT_ACCESSW(Pointer pointer) {
        super(pointer);
        read();
    }

    @Override
    protected List<String> getFieldOrder() {
        return Arrays.asList("grfAccessPermissions", "grfAccessMode", "grfInheritance", "Trustee");
    }
}
