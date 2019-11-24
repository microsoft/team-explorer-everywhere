package com.microsoft.tfs.jni.internal.winapi;

import com.sun.jna.Structure;
import com.sun.jna.platform.win32.WinBase;
import com.sun.jna.platform.win32.WinDef;

import java.util.Arrays;
import java.util.List;

public class WIN32_FILE_ATTRIBUTE_DATA extends Structure {
    public WinDef.DWORD dwFileAttributes;
    public WinBase.FILETIME ftCreationTime;
    public WinBase.FILETIME ftLastAccessTime;
    public WinBase.FILETIME ftLastWriteTime;
    public WinDef.DWORD nFileSizeHigh;
    public WinDef.DWORD nFileSizeLow;

    @Override protected List<String> getFieldOrder() {
        return Arrays.asList(
            "dwFileAttributes",
            "ftCreationTime",
            "ftLastAccessTime",
            "ftLastWriteTime",
            "nFileSizeHigh",
            "nFileSizeLow"
        );
    }
}
