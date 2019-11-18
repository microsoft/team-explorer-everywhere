package com.microsoft.tfs.jni.internal.console.winapi;

import com.sun.jna.Structure;

import java.util.Arrays;
import java.util.List;

public class Coord extends Structure {

    public static class ByValue extends Coord implements Structure.ByValue {
    }

    @Override protected List<String> getFieldOrder() {
        return Arrays.asList("x", "y");
    }

    public short x;
    public short y;
}
