package com.microsoft.tfs.jni.internal.console.unix;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Platform;
import com.sun.jna.Pointer;

public interface LibC extends Library {

    LibC INSTANCE = (LibC) Native.loadLibrary("c", LibC.class);

    int O_RDONLY = 0;
    long TIOCGWINSZ = Platform.isLinux() ? 0x5413L : 0x40087468L;

    int open(String pathname, int flags);

    int close(int fd);

    int ioctl(int fd, long cmd, Pointer p);
}
