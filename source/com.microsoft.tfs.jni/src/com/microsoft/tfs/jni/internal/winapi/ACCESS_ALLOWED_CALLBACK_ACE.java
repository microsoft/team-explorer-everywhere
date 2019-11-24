package com.microsoft.tfs.jni.internal.winapi;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.platform.win32.WinNT;

@Structure.FieldOrder({"Header", "Mask", "SidStart"})
public class ACCESS_ALLOWED_CALLBACK_ACE extends WinNT.ACCESS_ACEStructure {
    public WinNT.ACE_HEADER Header;
    public int Mask;
    public byte[] SidStart;

    public ACCESS_ALLOWED_CALLBACK_ACE(Pointer p) {
        super(p);
    }
}