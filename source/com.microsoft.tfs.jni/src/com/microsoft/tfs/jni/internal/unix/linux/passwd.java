package com.microsoft.tfs.jni.internal.unix.linux;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;

import java.util.Arrays;
import java.util.List;

public class passwd extends Structure {
    public passwd() {}

    public passwd(Pointer p) {
        super(p);
        read();
    }

    public String pw_name;       /* username */
    public String pw_passwd;     /* user password */
    public int pw_uid;        /* user ID */
    public int pw_gid;        /* group ID */
    public String pw_gecos;      /* user information */
    public String pw_dir;        /* home directory */
    public String pw_shell;      /* shell program */

    @Override protected List<String> getFieldOrder() {
        return Arrays.asList(
            "pw_name",
            "pw_passwd",
            "pw_uid",
            "pw_gid",
            "pw_gecos",
            "pw_dir",
            "pw_shell");
    }
}
