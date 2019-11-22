package com.microsoft.tfs.jni.internal.console;

import com.microsoft.tfs.jni.Console;
import com.microsoft.tfs.jni.internal.unix.LibC;
import com.microsoft.tfs.jni.internal.unix.termios;
import com.microsoft.tfs.jni.internal.unix.winsize;

class UnixNativeConsole implements Console {

    private final LibC libC;

    public UnixNativeConsole(LibC libC) {
        this.libC = libC;
    }

    private winsize getTtySize() {
        int tty = libC.open("/dev/tty", LibC.O_RDONLY);
        if (tty >= 0) {
            try {
                winsize size = new winsize();
                if (libC.ioctl(tty, LibC.TIOCGWINSZ, size) >= 0) {
                    return size;
                }
            } finally {
                libC.close(tty);
            }
        }

        return null;
    }

    @Override
    public int getConsoleColumns() {
        winsize size = getTtySize();
        return size == null ? 0 : size.ws_col;
    }

    @Override
    public int getConsoleRows() {
        winsize size = getTtySize();
        return size == null ? 0 : size.ws_row;
    }

    @Override
    public boolean disableEcho() {
        termios settings = new termios();
        if (libC.tcgetattr(LibC.STDIN_FILENO, settings) != 0)
            return false;

        settings.c_lflag &= ~LibC.ECHO;

        return libC.tcsetattr(LibC.STDIN_FILENO, LibC.TCSANOW, settings) == 0;
    }

    @Override
    public boolean enableEcho() {
        termios settings = new termios();
        if (libC.tcgetattr(LibC.STDIN_FILENO, settings) != 0)
            return false;

        settings.c_lflag |= LibC.ECHO;

        return libC.tcsetattr(LibC.STDIN_FILENO, LibC.TCSANOW, settings) == 0;
    }
}
