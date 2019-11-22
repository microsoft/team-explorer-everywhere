package com.microsoft.tfs.jni.internal.unix;

import com.sun.jna.Structure;

import java.util.Arrays;
import java.util.List;

public class termios extends Structure {

    @Override protected List<String> getFieldOrder() {
        return Arrays.asList("c_iflag", "c_oflag", "c_cflag", "c_lflag", "c_line", "c_cc", "c_ispeed", "c_ospeed");
    }

    public int c_iflag;                /* input mode flags */
    public int c_oflag;                /* output mode flags */
    public int c_cflag;                /* control mode flags */
    public int c_lflag;                /* local mode flags */
    public byte c_line;                /* line discipline */
    public byte[] c_cc = new byte[32]; /* control characters */
    public int c_ispeed;               /* input speed */
    public int c_ospeed;               /* output speed */
}
