// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.jni.internal.filesystem;

import java.text.MessageFormat;

import com.microsoft.tfs.jni.FileSystem;
import com.microsoft.tfs.jni.FileSystemAttributes;
import com.microsoft.tfs.util.Platform;

/**
 * A {@link FileSystem} implemented with external processes.
 */
public class ExecFileSystem implements FileSystem {
    private final FileSystem delegate;

    public ExecFileSystem() {
        if (Platform.isCurrentPlatform(Platform.WINDOWS)) {
            delegate = new WindowsExecFileSystem();
        } else if (Platform.isCurrentPlatform(Platform.GENERIC_UNIX)) {
            delegate = new UnixExecFileSystem();
        } else {
            throw new RuntimeException(
                MessageFormat.format(
                    "There is no ExecPlatformUtils functionality available for this platform ({0})", //$NON-NLS-1$
                    Platform.getCurrentPlatformString()));
        }
    }

    @Override
    public boolean createSymbolicLink(final String oldpath, final String newpath) {
        return delegate.createSymbolicLink(oldpath, newpath);
    }

    @Override
    public FileSystemAttributes getAttributes(final String filepath) {
        return delegate.getAttributes(filepath);
    }

    @Override
    public boolean setAttributes(final String filepath, final FileSystemAttributes attributes) {
        return delegate.setAttributes(filepath, attributes);
    }

    @Override
    public String getOwner(final String path) {
        return delegate.getOwner(path);
    }

    @Override
    public void setOwner(final String path, final String owner) {
        delegate.setOwner(path, owner);
    }

    @Override
    public void grantInheritableFullControl(
        final String path,
        final String user,
        final String copyExplicitRulesFromPath) {
        delegate.grantInheritableFullControl(path, user, copyExplicitRulesFromPath);
    }

    @Override
    public void copyExplicitDACLEntries(final String sourcePath, final String targetPath) {
        delegate.copyExplicitDACLEntries(sourcePath, targetPath);
    }

    @Override
    public void removeExplicitAllowEntries(final String path, final String user) {
        delegate.removeExplicitAllowEntries(path, user);
    }

    @Override
    public String[] listMacExtendedAttributes(final String filepath) {
        return delegate.listMacExtendedAttributes(filepath);
    }

    @Override
    public int readMacExtendedAttribute(
        final String filepath,
        final String attribute,
        final byte[] buffer,
        final int size,
        final long position) {
        return delegate.readMacExtendedAttribute(filepath, attribute, buffer, size, position);
    }

    @Override
    public boolean writeMacExtendedAttribute(
        final String filepath,
        final String attribute,
        final byte[] buffer,
        final int size,
        final long position) {
        return delegate.writeMacExtendedAttribute(filepath, attribute, buffer, size, position);
    }

    @Override
    public byte[] getMacExtendedAttribute(final String filepath, final String attribute) {
        return delegate.getMacExtendedAttribute(filepath, attribute);
    }

    @Override
    public boolean setMacExtendedAttribute(final String filepath, final String attribute, final byte[] value) {
        return delegate.setMacExtendedAttribute(filepath, attribute, value);
    }
}
