package com.microsoft.tfs.jni.internal.console.winapi;

import com.sun.jna.Structure;

import java.util.Arrays;
import java.util.List;

public class ConsoleScreenBufferInfo extends Structure {

    @Override protected List<String> getFieldOrder() {
        return Arrays.asList("dwSize", "dwCursorPosition", "wAttributes", "srWindow", "dwMaximumWindowSize");
    }

    public Coord dwSize;
    public Coord dwCursorPosition;
    public short wAttributes;
    public SmallRect srWindow;
    public Coord dwMaximumWindowSize;
}