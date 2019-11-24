package com.microsoft.tfs.jni.internal.winapi;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.platform.win32.Guid;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinNT;

@Structure.FieldOrder({"Header", "Mask", "Flags", "ObjectType", "InheritedObjectType", "SidStart"})
public class ACCESS_ALLOWED_CALLBACK_OBJECT_ACE extends WinNT.ACCESS_ACEStructure {
    public WinNT.ACE_HEADER Header;
    public int Mask;
    public WinDef.DWORD Flags;
    public Guid.GUID ObjectType;
    public Guid.GUID InheritedObjectType;
    public byte[] SidStart;

    public ACCESS_ALLOWED_CALLBACK_OBJECT_ACE(Pointer p) {
        super(p);
    }
}
