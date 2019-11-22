package com.microsoft.tfs.jni.internal.platformmisc;

import com.microsoft.tfs.jni.internal.unix.linux.LibC;
import com.microsoft.tfs.jni.internal.unix.linux.passwd;
import com.sun.jna.platform.linux.XAttr;
import com.sun.jna.ptr.PointerByReference;

public class LinuxNativePlatformMisc extends UnixNativePlatformMisc {

    private static final LibC libC = LibC.INSTANCE;

    protected LinuxNativePlatformMisc() {
        super(libC);
    }

    @Override
    public String getHomeDirectory(String username) {
        passwd pwd = new passwd();
        byte[] pwdBuffer = new byte[1024];
        PointerByReference tempPwdPtr = new PointerByReference();
        if (libC.getpwnam_r(username, pwd, pwdBuffer, new XAttr.size_t(pwdBuffer.length), tempPwdPtr) != 0
            || tempPwdPtr.getValue() == null)
            return null;

        passwd tempPwd = new passwd(tempPwdPtr.getValue());
        return tempPwd.pw_dir;
    }
}
