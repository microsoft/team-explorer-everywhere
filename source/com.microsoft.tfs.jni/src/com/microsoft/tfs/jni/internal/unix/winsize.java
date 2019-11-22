package com.microsoft.tfs.jni.internal.unix;

import com.sun.jna.Structure;

import java.util.Arrays;
import java.util.List;

public class winsize extends Structure {

    @Override protected List<String> getFieldOrder() {
        return Arrays.asList("ws_row", "ws_col", "ws_xpixel", "ws_ypixel");
    }

    public short ws_row;
    public short ws_col;
    public short ws_xpixel;
    public short ws_ypixel;
}
