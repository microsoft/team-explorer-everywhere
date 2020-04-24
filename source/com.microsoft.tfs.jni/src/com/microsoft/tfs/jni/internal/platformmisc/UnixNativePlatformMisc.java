package com.microsoft.tfs.jni.internal.platformmisc;

import com.microsoft.tfs.jni.PlatformMisc;
import com.microsoft.tfs.jni.internal.unix.LibC;
import com.sun.jna.platform.unix.LibCAPI;

import java.nio.charset.Charset;

public abstract class UnixNativePlatformMisc implements PlatformMisc {

    private final LibC libC;

    protected UnixNativePlatformMisc(LibC libC) {
        this.libC = libC;
    }

    @Override
    public boolean changeCurrentDirectory(String directory) {
        return libC.chdir(directory) == 0;
    }

    @Override
    public int getDefaultCodePage() {
        throw new RuntimeException("Platform not supported");
    }

    @Override
    public String getComputerName() {
        byte[] buffer = new byte[LibCAPI.HOST_NAME_MAX + 1]; // + 1 for terminating zero
        if (libC.gethostname(buffer, buffer.length) != 0) {
            return null;
        }

        int length = buffer.length;
        for (int i = 0; i < buffer.length; ++i) {
            if (buffer[i] == '\0') {
                length = i;
                break;
            }
        }

        return new String(buffer, 0, length, Charset.defaultCharset());
    }

    @Override
    public String getEnvironmentVariable(String name) {
        return System.getenv(name);
    }

    @Override
    public String expandEnvironmentString(String value) {
        throw new RuntimeException("Platform not supported");
    }

    @Override
    public String getCurrentIdentityUser() {
        throw new RuntimeException("Platform not supported");
    }

    @Override
    public String getWellKnownSID(int wellKnownSIDType, String domainSIDString) {
        throw new RuntimeException("Platform not supported");
    }
}
