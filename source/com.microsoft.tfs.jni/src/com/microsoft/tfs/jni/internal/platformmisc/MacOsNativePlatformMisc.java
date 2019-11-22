package com.microsoft.tfs.jni.internal.platformmisc;

import com.microsoft.tfs.jni.internal.unix.macos.*;
import com.sun.jna.platform.linux.XAttr;
import com.sun.jna.platform.mac.CoreFoundation;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;

public class MacOsNativePlatformMisc extends UnixNativePlatformMisc {

    private static final LibC libC = LibC.INSTANCE;
    private static final CoreFoundation coreFoundation = CoreFoundation.INSTANCE;
    private static final CoreServices coreServices = CoreServices.INSTANCE;
    private static final SystemConfiguration systemConfiguration = SystemConfiguration.INSTANCE;

    protected MacOsNativePlatformMisc() {
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

    /**
     * This will attempt to return the HostName system preference (10.4+). If that fails, this will attempt to return
     * the LocalHostName system preference (aka, the Bonjour name as configured in Sharing preferences; 10.3+). This is
     * to work around MacOS's dynamic hostname. If both of those fail this uses the Unix hostname.
     */
    @Override
    public String getComputerName() {
        IntByReference osVersion = new IntByReference();
        if (coreServices.Gestalt(CoreServices.gestaltSystemVersion, osVersion) != 0)
            osVersion.setValue(0);

        if (osVersion.getValue() >= 0x1040) {
            CoreFoundation.CFStringRef microsoft = CoreFoundation.CFStringRef.createCFString("Microsoft");
            try {
                SCPreferencesRef configuration = systemConfiguration.SCPreferencesCreate(null, microsoft, null);
                if (configuration != null) {
                    try {
                        CoreFoundation.CFStringRef hostName =
                            systemConfiguration.SCPreferencesGetHostName(configuration);
                        if (hostName != null) {
                            return hostName.stringValue();
                        }
                    } finally {
                        coreFoundation.CFRelease(configuration);
                    }
                }
            } finally {
                CoreFoundation.INSTANCE.CFRelease(microsoft);
            }
        }

        if (osVersion.getValue() >= 0x1030) {
            CoreFoundation.CFStringRef hostName = systemConfiguration.SCDynamicStoreCopyLocalHostName(null);
            if (hostName != null) {
                try {
                    return hostName.stringValue();
                } finally {
                    coreFoundation.CFRelease(hostName);
                }
            }
        }

        return super.getComputerName();
    }
}
