package com.microsoft.tfs.jni.internal.wincredential.winapi;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.WString;
import com.sun.jna.platform.win32.WinBase;
import com.sun.jna.platform.win32.WinDef;

import java.util.Arrays;
import java.util.List;

public class CREDENTIALW extends Structure {

    public CREDENTIALW() {
    }

    public CREDENTIALW(Pointer p) {
        super(p);
        read();
    }

    @Override protected List<String> getFieldOrder() {
        return Arrays.asList(
            "Flags",
            "Type",
            "TargetName",
            "Comment",
            "LastWritten",
            "CredentialBlobSize",
            "CredentialBlob",
            "Persist",
            "AttributeCount",
            "Attributes",
            "TargetAlias",
            "UserName"
        );
    }

    public WinDef.DWORD Flags;
    public WinDef.DWORD Type;
    public WString TargetName;
    public WString Comment;
    public WinBase.FILETIME LastWritten;
    public WinDef.DWORD CredentialBlobSize;
    public Pointer CredentialBlob;
    public WinDef.DWORD Persist;
    public WinDef.DWORD AttributeCount;
    public Pointer Attributes;
    public WString TargetAlias;
    public WString UserName;
}
