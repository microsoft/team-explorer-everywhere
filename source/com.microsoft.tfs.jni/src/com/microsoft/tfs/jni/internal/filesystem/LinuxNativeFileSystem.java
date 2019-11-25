package com.microsoft.tfs.jni.internal.filesystem;

import com.microsoft.tfs.jni.internal.unix.linux.LibC;

public class LinuxNativeFileSystem extends UnixNativeFileSystem {
    public LinuxNativeFileSystem() {
        super(LibC.INSTANCE);
    }
}
