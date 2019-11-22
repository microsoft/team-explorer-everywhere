package com.microsoft.tfs.jni.internal.unix.macos;

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

    public String pw_name;       /* user name */
    public String pw_passwd;     /* encrypted password */
    public int pw_uid;           /* user uid */
    public int pw_gid;           /* user gid */
    public long pw_change;       /* password change time */
    public String pw_class;      /* user access class */
    public String pw_gecos;      /* Honeywell login info */
    public String pw_dir;        /* home directory */
    public String pw_shell;      /* default shell */
    public long pw_expire;       /* account expiration */
    public int pw_fields;        /* internal: fields filled in */

    @Override protected List<String> getFieldOrder() {
        return Arrays.asList(
            "pw_name",
            "pw_passwd",
            "pw_uid",
            "pw_gid",
            "pw_change",
            "pw_class",
            "pw_gecos",
            "pw_dir",
            "pw_shell",
            "pw_expire",
            "pw_fields");
    }
}
