package com.microsoft.tfs.jni.internal.filesystem;

import com.microsoft.tfs.jni.internal.unix.macos.LibC;

public class MacOsNativeFileSystem extends UnixNativeFileSystem {
    public MacOsNativeFileSystem() {
        super(LibC.INSTANCE);
    }

    @Override
    public String[] listMacExtendedAttributes(String filepath) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public int readMacExtendedAttribute(String filepath, String attribute, byte[] buffer, int size, long position) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public boolean writeMacExtendedAttribute(
        String filepath,
        String attribute,
        byte[] buffer,
        int size,
        long position) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public byte[] getMacExtendedAttribute(String filepath, String attribute) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public boolean setMacExtendedAttribute(String filepath, String attribute, byte[] value) {
        throw new RuntimeException("Not implemented");
    }
}
