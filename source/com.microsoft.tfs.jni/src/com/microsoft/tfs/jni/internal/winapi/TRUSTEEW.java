package com.microsoft.tfs.jni.internal.winapi;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;

import java.util.Arrays;
import java.util.List;

public class TRUSTEEW extends Structure {
    public Pointer pMultipleTrustee;
    public int MultipleTrusteeOperation;
    public int TrusteeForm;
    public int TrusteeType;
    public Pointer ptstrName;

    @Override protected List<String> getFieldOrder() {
        return Arrays.asList(
            "pMultipleTrustee",
            "MultipleTrusteeOperation",
            "TrusteeForm",
            "TrusteeType",
            "ptstrName"
        );
    }
}
