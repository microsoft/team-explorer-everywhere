package com.microsoft.tfs.jni.internal.unix;

import com.sun.jna.Native;
import com.sun.jna.Platform;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.platform.linux.XAttr;
import com.sun.jna.ptr.PointerByReference;

public interface LibC extends com.sun.jna.platform.unix.LibC {
    int O_RDONLY = 0;
    long TIOCGWINSZ = Platform.isLinux() ? 0x5413L : 0x40087468L;
    int STDIN_FILENO = 0;
    int ECHO = 8;
    int TCSANOW = 0;

    int open(String pathname, int flags);
    int close(int fd);

    int ioctl(int fd, long cmd, winsize p);

    int tcgetattr(int fd, termios termios_p);
    int tcsetattr(int fd, int optional_actions, termios termios_p);

    int chdir(String path);
}
