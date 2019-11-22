package com.microsoft.tfs.jni.internal.unix.macos;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.ptr.IntByReference;

public interface CoreServices extends Library {
    CoreServices INSTANCE = Native.load("CoreServices", CoreServices.class);

    int gestaltSystemVersion = 0x73797376; // 'sysv'

    short Gestalt(int selector, IntByReference response);
}
