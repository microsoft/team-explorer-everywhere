package com.microsoft.tfs.jni.internal.winapi;

import com.sun.jna.Structure;
import com.sun.jna.platform.win32.WinDef;

import java.util.Arrays;
import java.util.List;

public class ACL_SIZE_INFORMATION extends Structure {
    public WinDef.DWORD AceCount;
    public WinDef.DWORD AclBytesInUse;
    public WinDef.DWORD AclBytesFree;

    @Override
    protected List<String> getFieldOrder() {
        return Arrays.asList(
            "AceCount",
            "AclBytesInUse",
            "AclBytesFree");
    }
}
