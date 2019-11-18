package com.microsoft.tfs.jni.internal.console;

import com.microsoft.tfs.jni.Console;
import com.microsoft.tfs.jni.internal.console.unix.LibC;
import com.microsoft.tfs.jni.internal.console.unix.winsize;

class UnixNativeConsole implements Console {

    private winsize getTtySize() {
        LibC libC = LibC.INSTANCE;
        int tty = libC.open("/dev/tty", LibC.O_RDONLY);
        if (tty >= 0) {
            try {
                winsize size = new winsize();
                if (libC.ioctl(tty, LibC.TIOCGWINSZ, size.getPointer()) >= 0) {
                    return size;
                }
            } finally {
                libC.close(tty);
            }
        }

        return null;
    }

    @Override public int getConsoleColumns() {
        winsize size = getTtySize();
        return size == null ? 0 : size.ws_col;
    }

    @Override public int getConsoleRows() {
        winsize size = getTtySize();
        return size == null ? 0 : size.ws_row;
    }

    @Override public boolean disableEcho() {
        return false;
    }

    @Override public boolean enableEcho() {
        return false;
    }
}
