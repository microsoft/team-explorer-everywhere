// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.jni;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;

import com.microsoft.tfs.jni.internal.filesystem.NativeFileSystem;
import com.microsoft.tfs.util.Check;

public class FileSystemUtils implements FileSystem {
    private static final FileSystemUtils instance = new FileSystemUtils();

    /**
     * @return an instance of a {@link FileSystem} implementation full of
     *         utility methods that are ready-to-call.
     */
    public static FileSystemUtils getInstance() {
        return FileSystemUtils.instance;
    }

    private final NativeFileSystem nativeImpl;

    private FileSystemUtils() {
        nativeImpl = new NativeFileSystem();
    }

    @Override
    public FileSystemAttributes getAttributes(final String filepath) {
        try {
            final FileSystemAttributes attrs = nativeImpl.getAttributes(filepath);

            if (attrs.isSymbolicLink()) {
                attrs.setArchive(false);
                attrs.setExecutable(false);
                attrs.setHidden(false);
                attrs.setNotContentIndexed(false);
                attrs.setOwnerOnly(false);
                attrs.setPublicWritable(false);
                attrs.setReadOnly(true);
                attrs.setSystem(false);
            }

            return attrs;
        } catch (final RuntimeException e) {
            // Add the file path to the exception message.
            final String format = Messages.getString("FileSystemUtils.MessagePlusPathFormat"); //$NON-NLS-1$
            throw new RuntimeException(MessageFormat.format(format, e.getLocalizedMessage(), filepath));
        }
    }

    /**
     * Convenience method to get attributes from a {@link File}.
     *
     * @see #getAttributes(String)
     */
    public FileSystemAttributes getAttributes(final File file) {
        /*
         * Use getPath() instead of getFullPath() to skip the filesystem check
         * for speed. This method is often used in tight scanning loops where
         * speed is important.
         */
        Check.notNull(file, "file"); //$NON-NLS-1$
        return this.getAttributes(file.getPath());
    }

    @Override
    public boolean setAttributes(final String filepath, final FileSystemAttributes attributes) {
        final FileSystemAttributes attrs = getAttributes(filepath);
        if (attrs.isSymbolicLink()) {
            return true;
        }

        return nativeImpl.setAttributes(filepath, attributes);
    }

    /**
     * Convenience method to set attributes for a {@link File}.
     *
     * @see #setAttributes(String, FileSystemAttributes)
     */
    public boolean setAttributes(final File file, final FileSystemAttributes attributes) {
        Check.notNull(file, "file"); //$NON-NLS-1$
        /*
         * Use getPath() instead of getFullPath() to skip the filesystem check
         * for speed. This method is often used in tight scanning loops where
         * speed is important.
         */
        return setAttributes(file.getPath(), attributes);
    }

    @Override
    public String getOwner(final String path) {
        Check.notNull(path, "path"); //$NON-NLS-1$
        return nativeImpl.getOwner(path);
    }

    @Override
    public void setOwner(final String path, final String owner) {
        Check.notNull(path, "path"); //$NON-NLS-1$
        Check.notNull(owner, "owner"); //$NON-NLS-1$
        nativeImpl.setOwner(path, owner);
    }

    @Override
    public void grantInheritableFullControl(
        final String path,
        final String user,
        final String copyExplicitRulesFromPath) {
        Check.notNull(path, "path"); //$NON-NLS-1$
        Check.notNull(user, "user"); //$NON-NLS-1$

        nativeImpl.grantInheritableFullControl(path, user, copyExplicitRulesFromPath);
    }

    @Override
    public void copyExplicitDACLEntries(final String sourcePath, final String targetPath) {
        Check.notNull(sourcePath, "sourcePath"); //$NON-NLS-1$
        Check.notNull(targetPath, "targetPath"); //$NON-NLS-1$

        nativeImpl.copyExplicitDACLEntries(sourcePath, targetPath);
    }

    @Override
    public void removeExplicitAllowEntries(final String path, final String user) {
        Check.notNull(path, "path"); //$NON-NLS-1$
        Check.notNull(user, "user"); //$NON-NLS-1$

        nativeImpl.removeExplicitAllowEntries(path, user);
    }

    @Override
    public boolean createSymbolicLink(final String oldpath, final String newpath) {
        return nativeImpl.createSymbolicLink(oldpath, newpath);
    }

    public String getSymbolicLink(final String path) {
        return nativeImpl.getSymbolicLink(path);
    }

    public File createTempFileSecure(final String prefix, final String suffix, final File parentFile)
        throws IOException {
        return nativeImpl.createTempFileSecure(prefix, suffix, parentFile);
    }

    @Override
    public String[] listMacExtendedAttributes(final String filepath) {
        return nativeImpl.listMacExtendedAttributes(filepath);
    }

    @Override
    public int readMacExtendedAttribute(
        final String filepath,
        final String attribute,
        final byte[] buffer,
        final int size,
        final long position) {
        return nativeImpl.readMacExtendedAttribute(filepath, attribute, buffer, size, position);
    }

    @Override
    public boolean writeMacExtendedAttribute(
        final String filepath,
        final String attribute,
        final byte[] buffer,
        final int size,
        final long position) {
        return nativeImpl.writeMacExtendedAttribute(filepath, attribute, buffer, size, position);
    }

    @Override
    public byte[] getMacExtendedAttribute(final String filepath, final String attribute) {
        return nativeImpl.getMacExtendedAttribute(filepath, attribute);
    }

    @Override
    public boolean setMacExtendedAttribute(final String filepath, final String attribute, final byte[] value) {
        return nativeImpl.setMacExtendedAttribute(filepath, attribute, value);
    }
}
