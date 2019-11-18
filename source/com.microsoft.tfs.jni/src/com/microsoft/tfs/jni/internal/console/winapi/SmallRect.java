package com.microsoft.tfs.jni.internal.console.winapi;

import com.sun.jna.Structure;

import java.util.Arrays;
import java.util.List;

public class SmallRect extends Structure {

    @Override protected List<String> getFieldOrder() {
        return Arrays.asList("left", "top", "right", "bottom");
    }

    public short left;
    public short top;
    public short right;
    public short bottom;
}
