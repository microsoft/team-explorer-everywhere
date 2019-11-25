package com.microsoft.tfs.jni.internal.unix;

import com.sun.jna.Platform;
import com.sun.jna.platform.linux.XAttr;

public interface LibC extends com.sun.jna.platform.unix.LibC {
    int O_RDONLY = 0;
    long TIOCGWINSZ = Platform.isLinux() ? 0x5413L : 0x40087468L;
    int S_IRUSR = 256;
    int S_IWUSR = 128;
    int S_IXUSR = 64;
    int S_IRGRP = 32;
    int S_IWGRP = 16;
    int S_IXGRP = 8;
    int S_IROTH = 4;
    int S_IWOTH = 2;
    int S_IXOTH = 1;
    int STDIN_FILENO = 0;
    int ECHO = 8;
    int TCSANOW = 0;

    int open(String pathname, int flags);
    int close(int fd);

    int umask(int mask);
    int chmod(String path, int mode);
    int symlink(String target, String linkpath);
    XAttr.size_t readlink(String path, byte[] buf, XAttr.size_t bufsiz);

    int ioctl(int fd, long cmd, winsize p);

    int tcgetattr(int fd, termios termios_p);
    int tcsetattr(int fd, int optional_actions, termios termios_p);

    int chdir(String path);
}
