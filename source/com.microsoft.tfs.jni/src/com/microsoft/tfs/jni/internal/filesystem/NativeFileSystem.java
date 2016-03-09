// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.jni.internal.filesystem;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.jni.FileSystem;
import com.microsoft.tfs.jni.FileSystemAttributes;
import com.microsoft.tfs.jni.internal.LibraryNames;
import com.microsoft.tfs.jni.loader.NativeLoader;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.Platform;

/**
 * An implementation of the {@link FileSystem} interface that uses native
 * methods.
 *
 * @threadsafety thread-safe
 */
public class NativeFileSystem implements FileSystem {
    /**
     * This static initializer is a "best-effort" native code loader (no
     * exceptions thrown for normal load failures).
     *
     * Apps with multiple classloaders (like Eclipse) can run this initializer
     * more than once in a single JVM OS process, and on some platforms
     * (Windows) the native libraries will fail to load the second time, because
     * they're already loaded. This failure can be ignored because the native
     * code will execute fine.
     */
    static {
        NativeLoader.loadLibraryAndLogError(LibraryNames.FILESYSTEM_LIBRARY_NAME);
    }

    /**
     * This object exists so that only one call to a umask-changing function is
     * allowed to run at one time. On Unix, umask cannot be queried without
     * being changed, so it must be changed right back and this introduces a
     * race in the native code. We lock here for simplicity.
     * <p>
     * This lock must be held whenever code is executing that may change (set)
     * the mode of a file (including readable, writable, executable, etc.).
     */
    private static final Object umaskLock = new Object();

    private static final Log log = LogFactory.getLog(NativeFileSystem.class);

    public NativeFileSystem() {
    }

    @Override
    public FileSystemAttributes getAttributes(final String filepath) {
        Check.notNull(filepath, "filepath"); //$NON-NLS-1$

        if (log.isTraceEnabled()) {
            log.trace(MessageFormat.format("ENTER getAttributes({0})", filepath)); //$NON-NLS-1$
        }

        try {
            return nativeGetAttributes(filepath);
        } finally {
            if (log.isTraceEnabled()) {
                log.trace(MessageFormat.format("EXIT getAttributes({0})", filepath)); //$NON-NLS-1$
            }
        }
    }

    @Override
    public boolean setAttributes(final String filepath, final FileSystemAttributes attributes) {
        Check.notNull(filepath, "filepath"); //$NON-NLS-1$

        if (log.isTraceEnabled()) {
            log.trace(MessageFormat.format("ENTER setAttributes({0}, {1})", filepath, attributes)); //$NON-NLS-1$
        }

        try {
            synchronized (umaskLock) {
                return nativeSetAttributes(filepath, attributes);
            }
        } finally {
            if (log.isTraceEnabled()) {
                log.trace(MessageFormat.format("EXIT setAttributes({0}, {1})", filepath, attributes)); //$NON-NLS-1$
            }
        }
    }

    @Override
    public String getOwner(final String path) {
        Check.notNull(path, "path"); //$NON-NLS-1$

        if (Platform.isCurrentPlatform(Platform.WINDOWS) == false) {
            return null;
        }

        if (log.isTraceEnabled()) {
            log.trace(MessageFormat.format("ENTER getOwner({0})", path)); //$NON-NLS-1$
        }

        try {
            return nativeGetOwner(path);
        } finally {
            if (log.isTraceEnabled()) {
                log.trace(MessageFormat.format("EXIT getOwner({0})", path)); //$NON-NLS-1$
            }
        }
    }

    @Override
    public void setOwner(final String path, final String owner) {
        Check.notNull(path, "path"); //$NON-NLS-1$
        Check.notNull(owner, "owner"); //$NON-NLS-1$

        if (Platform.isCurrentPlatform(Platform.WINDOWS) == false) {
            return;
        }

        if (log.isTraceEnabled()) {
            log.trace(MessageFormat.format("ENTER setOwner({0}, {1})", path, owner)); //$NON-NLS-1$
        }

        try {
            nativeSetOwner(path, owner);
        } finally {
            if (log.isTraceEnabled()) {
                log.trace(MessageFormat.format("EXIT setOwner({0}, {1})", path, owner)); //$NON-NLS-1$
            }
        }
    }

    @Override
    public void grantInheritableFullControl(
        final String path,
        final String user,
        final String copyExplicitRulesFromPath) {
        if (Platform.isCurrentPlatform(Platform.WINDOWS) == false) {
            return;
        }

        if (log.isTraceEnabled()) {
            log.trace(MessageFormat.format(
                "ENTER grantInheritableFullControl({0}, {1}, {2})", //$NON-NLS-1$
                path,
                user,
                copyExplicitRulesFromPath));
        }

        try {
            nativeGrantInheritableFullControl(path, user, copyExplicitRulesFromPath);
        } finally {
            if (log.isTraceEnabled()) {
                log.trace(MessageFormat.format(
                    "EXIT grantInheritableFullControl({0}, {1}, {2})", //$NON-NLS-1$
                    path,
                    user,
                    copyExplicitRulesFromPath));
            }
        }
    }

    @Override
    public void copyExplicitDACLEntries(final String sourcePath, final String targetPath) {
        if (Platform.isCurrentPlatform(Platform.WINDOWS) == false) {
            return;
        }

        if (log.isTraceEnabled()) {
            log.trace(MessageFormat.format(
                "ENTER copyExplicitDACLs({0}, {1})", //$NON-NLS-1$
                sourcePath,
                targetPath));
        }

        try {
            nativeCopyExplicitDACLEntries(sourcePath, targetPath);
        } finally {
            if (log.isTraceEnabled()) {
                log.trace(MessageFormat.format(
                    "EXIT copyExplicitDACLs({0}, {1})", //$NON-NLS-1$
                    sourcePath,
                    targetPath));
            }
        }
    }

    @Override
    public void removeExplicitAllowEntries(final String path, final String user) {
        if (Platform.isCurrentPlatform(Platform.WINDOWS) == false) {
            return;
        }

        if (log.isTraceEnabled()) {
            log.trace(MessageFormat.format(
                "ENTER removeExplicitAllowEntries({0}, {1})", //$NON-NLS-1$
                path,
                user));
        }

        try {
            nativeRemoveExplicitAllowEntries(path, user);
        } finally {
            if (log.isTraceEnabled()) {
                log.trace(MessageFormat.format(
                    "EXIT removeExplicitAllowEntries({0}, {1})", //$NON-NLS-1$
                    path,
                    user));
            }
        }
    }

    @Override
    public boolean createSymbolicLink(final String oldpath, final String newpath) {
        Check.notNull(oldpath, "oldpath"); //$NON-NLS-1$
        Check.notNull(newpath, "newpath"); //$NON-NLS-1$

        if (Platform.isCurrentPlatform(Platform.GENERIC_UNIX) == false) {
            return false;
        }

        if (log.isTraceEnabled()) {
            log.trace(MessageFormat.format("ENTER createSymbolicLink({0}, {1})", oldpath, newpath)); //$NON-NLS-1$
        }

        try {
            return nativeCreateSymbolicLink(oldpath, newpath) == 0;
        } finally {
            if (log.isTraceEnabled()) {
                log.trace(MessageFormat.format("EXIT createSymbolicLink({0}, {1})", oldpath, newpath)); //$NON-NLS-1$
            }
        }
    }

    public String getSymbolicLink(final String path) {
        Check.notNull(path, "path"); //$NON-NLS-1$
        if (Platform.isCurrentPlatform(Platform.GENERIC_UNIX) == false) {
            return null;
        }

        if (log.isTraceEnabled()) {
            log.trace(MessageFormat.format("ENTER getSymbolicLink({0})", path)); //$NON-NLS-1$
        }

        try {
            return nativeGetSymbolicLink(path);
        } finally {
            if (log.isTraceEnabled()) {
                log.trace(MessageFormat.format("EXIT getSymbolicLink({0})", path)); //$NON-NLS-1$
            }
        }
    }

    public File createTempFileSecure(final String prefix, final String suffix) throws IOException {
        return createTempFileSecure(prefix, suffix, null);
    }

    public File createTempFileSecure(final String prefix, String suffix, File directory) throws IOException {
        /* TODO: we should do this on Windows with ACLs? */
        if (Platform.isCurrentPlatform(Platform.GENERIC_UNIX) == false) {
            return File.createTempFile(prefix, suffix, directory);
        }

        Check.notNull(prefix, "prefix"); //$NON-NLS-1$
        Check.isTrue(prefix.length() >= 3, "prefix.length() >= 3"); //$NON-NLS-1$

        if (suffix == null) {
            suffix = ".tmp"; //$NON-NLS-1$
        }

        if (directory == null) {
            final String tempPath = System.getProperty("java.io.tmpdir"); //$NON-NLS-1$

            if (tempPath == null) {
                throw new IOException("Could not determine temporary directory"); //$NON-NLS-1$
            }

            directory = new File(tempPath);

            Check.isTrue(directory.isDirectory(), "directory.isDirectory()"); //$NON-NLS-1$
        }

        final String filename = nativeCreateTempFileSecure(prefix, suffix, directory.getAbsolutePath());

        Check.notNull(filename, "filename"); //$NON-NLS-1$

        return new File(filename);
    }

    @Override
    public String[] listMacExtendedAttributes(final String filepath) {
        Check.notNull(filepath, "filepath"); //$NON-NLS-1$

        if (Platform.isCurrentPlatform(Platform.MAC_OS_X)) {
            if (log.isTraceEnabled()) {
                log.trace(MessageFormat.format("ENTER listMacExtendedAttributes({0})", filepath)); //$NON-NLS-1$
            }

            try {
                return nativeListMacExtendedAttributes(filepath);
            } finally {
                if (log.isTraceEnabled()) {
                    log.trace(MessageFormat.format("EXIT listMacExtendedAttributes({0})", filepath)); //$NON-NLS-1$
                }
            }
        }

        return null;
    }

    @Override
    public int readMacExtendedAttribute(
        final String filepath,
        final String attribute,
        final byte[] buffer,
        final int size,
        final long position) {
        Check.notNull(filepath, "filepath"); //$NON-NLS-1$
        Check.notNull(attribute, "attribute"); //$NON-NLS-1$
        Check.notNull(buffer, "buffer"); //$NON-NLS-1$

        if (Platform.isCurrentPlatform(Platform.MAC_OS_X)) {
            if (log.isTraceEnabled()) {
                log.trace(MessageFormat.format(
                    "ENTER readMacExtendedAttribute({0}, {1}, ..., {2}, {3})", //$NON-NLS-1$
                    filepath,
                    attribute,
                    size,
                    position));
            }

            try {
                return nativeReadMacExtendedAttribute(filepath, attribute, buffer, size, position);
            } finally {
                if (log.isTraceEnabled()) {
                    log.trace(MessageFormat.format(
                        "EXIT readMacExtendedAttribute({0}, {1}, ..., {2}, {3})", //$NON-NLS-1$
                        filepath,
                        attribute,
                        size,
                        position));
                }
            }
        }

        return -1;
    }

    @Override
    public boolean writeMacExtendedAttribute(
        final String filepath,
        final String attribute,
        final byte[] buffer,
        final int size,
        final long position) {
        Check.notNull(filepath, "filepath"); //$NON-NLS-1$
        Check.notNull(attribute, "attribute"); //$NON-NLS-1$
        Check.notNull(buffer, "buffer"); //$NON-NLS-1$

        if (Platform.isCurrentPlatform(Platform.MAC_OS_X)) {
            if (log.isTraceEnabled()) {
                log.trace(MessageFormat.format(
                    "ENTER writeMacExtendedAttribute({0}, {1}, ..., {2}, {3})", //$NON-NLS-1$
                    filepath,
                    attribute,
                    size,
                    position));
            }

            try {
                return nativeWriteMacExtendedAttribute(filepath, attribute, buffer, size, position);
            } finally {
                if (log.isTraceEnabled()) {
                    log.trace(MessageFormat.format(
                        "EXIT writeMacExtendedAttribute({0}, {1}, ..., {2}, {3})", //$NON-NLS-1$
                        filepath,
                        attribute,
                        size,
                        position));
                }
            }
        }

        return false;
    }

    @Override
    public byte[] getMacExtendedAttribute(final String filepath, final String attribute) {
        Check.notNull(filepath, "filepath"); //$NON-NLS-1$
        Check.notNull(attribute, "attribute"); //$NON-NLS-1$

        if (Platform.isCurrentPlatform(Platform.MAC_OS_X)) {
            if (log.isTraceEnabled()) {
                log.trace(MessageFormat.format("ENTER getMacExtendedAttribute({0}, {1})", filepath, attribute)); //$NON-NLS-1$
            }

            try {
                return nativeGetMacExtendedAttribute(filepath, attribute);
            } finally {
                if (log.isTraceEnabled()) {
                    log.trace(MessageFormat.format("EXIT getMacExtendedAttribute({0}, {1})", filepath, attribute)); //$NON-NLS-1$
                }
            }
        }

        return null;
    }

    @Override
    public boolean setMacExtendedAttribute(final String filepath, final String attribute, final byte[] value) {
        Check.notNull(filepath, "filepath"); //$NON-NLS-1$
        Check.notNull(attribute, "attribute"); //$NON-NLS-1$
        Check.notNull(value, "value"); //$NON-NLS-1$

        if (Platform.isCurrentPlatform(Platform.MAC_OS_X)) {
            if (log.isTraceEnabled()) {
                log.trace(MessageFormat.format("ENTER setMacExtendedAttribute({0}, {1}, ...)", filepath, attribute)); //$NON-NLS-1$
            }

            try {
                return nativeSetMacExtendedAttribute(filepath, attribute, value);
            } finally {
                if (log.isTraceEnabled()) {
                    log.trace(MessageFormat.format("EXIT setMacExtendedAttribute({0}, {1}, ...)", filepath, attribute)); //$NON-NLS-1$
                }
            }
        }

        return false;
    }

    private static native FileSystemAttributes nativeGetAttributes(String filepath);

    private static native boolean nativeSetAttributes(String filepath, FileSystemAttributes attributes);

    // WARNING: Following are only available on Windows.

    private static native String nativeGetOwner(String path);

    private static native void nativeSetOwner(String path, String user);

    private static native void nativeGrantInheritableFullControl(
        String path,
        String user,
        String copyExistingRulesFromPath);

    private static native void nativeCopyExplicitDACLEntries(String sourcePath, String targetPath);

    private static native void nativeRemoveExplicitAllowEntries(String path, String user);

    // WARNING: Following are only available on Unix.

    private static native String nativeCreateTempFileSecure(String prefix, String suffix, String parentFile);

    private static native int nativeCreateSymbolicLink(String oldpath, String newpath);

    private static native String nativeGetSymbolicLink(String filePath);

    // WARNING: Following are only available on OS X.

    private static native String[] nativeListMacExtendedAttributes(String filename);

    private static native int nativeReadMacExtendedAttribute(
        String filename,
        String attribute,
        byte[] buffer,
        int size,
        long position);

    private static native boolean nativeWriteMacExtendedAttribute(
        String filename,
        String attribute,
        byte[] buffer,
        int size,
        long position);

    private static native byte[] nativeGetMacExtendedAttribute(String filename, String attribute);

    private static native boolean nativeSetMacExtendedAttribute(String filename, String attribute, byte[] value);
}
