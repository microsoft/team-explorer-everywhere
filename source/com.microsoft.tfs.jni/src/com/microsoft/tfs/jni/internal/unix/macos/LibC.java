package com.microsoft.tfs.jni.internal.unix.macos;

import com.sun.jna.Native;
import com.sun.jna.platform.linux.XAttr;
import com.sun.jna.ptr.PointerByReference;

public interface LibC extends com.microsoft.tfs.jni.internal.unix.LibC {
    LibC INSTANCE = Native.load("c", LibC.class);

    int getpwnam_r(String name, passwd pwd, byte[] buf, XAttr.size_t buflen, PointerByReference result);
}
