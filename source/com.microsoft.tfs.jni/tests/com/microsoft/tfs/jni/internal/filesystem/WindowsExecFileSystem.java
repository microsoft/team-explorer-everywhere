// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.jni.internal.filesystem;

import java.io.File;
import java.text.MessageFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.jni.ExecHelpers;
import com.microsoft.tfs.jni.FileSystem;
import com.microsoft.tfs.jni.FileSystemAttributes;
import com.microsoft.tfs.jni.FileSystemTime;
import com.microsoft.tfs.util.Check;

/**
 * An implementation of the {@link FileSystem} interface via external Windows
 * process execution.
 */
public class WindowsExecFileSystem implements FileSystem {
    private final static Log log = LogFactory.getLog(WindowsExecFileSystem.class);

    @Override
    public FileSystemAttributes getAttributes(final String filepath) {
        Check.notNull(filepath, "filepath"); //$NON-NLS-1$

        final File file = new File(filepath);

        /*
         * Using Java's File class is faster than spawning an external process.
         *
         * This method for querying writable status fails on Unix when running
         * as root. Many Java implementations return "true" for all files
         * regardless of permissions set on the file when root.
         *
         * Note that this does take the Mac immutable flag into account.
         */
        final long lastModifiedMillis = file.lastModified();

        return new FileSystemAttributes(
            file.exists(),
            new FileSystemTime(lastModifiedMillis / 1000, (lastModifiedMillis % 1000) * 1000000),
            file.length(),
            file.canWrite() == false,
            false,
            false,
            false,
            false,
            file.isDirectory(),
            false,
            false,
            true,
            false);
    }

    @Override
    public boolean setAttributes(final String filepath, final FileSystemAttributes attributes) {
        Check.notNull(filepath, "filepath"); //$NON-NLS-1$
        Check.notNull(attributes, "attributes"); //$NON-NLS-1$

        /* Query the current attributes to discover what we need to modify. */
        final FileSystemAttributes oldAttributes = getAttributes(filepath);

        if (oldAttributes == null) {
            return false;
        }

        /*
         * Note that attributes that we don't understand (ie, "archive") are
         * ignored.
         */
        if (oldAttributes.isReadOnly() != attributes.isReadOnly()) {
            if (attributes.isReadOnly()) {
                return (new File(filepath).setReadOnly());
            } else {
                final String[] args = new String[] {
                    "attrib", //$NON-NLS-1$
                    "-r", //$NON-NLS-1$
                    filepath
                };

                final int ret = ExecHelpers.exec(args, null);

                if (ret != 0) {
                    log.error(MessageFormat.format(
                        "External command returned non-zero exit status {0}: {1}", //$NON-NLS-1$
                        Integer.toString(ret),
                        ExecHelpers.buildCommandForError(args)));
                    return false;
                }
            }
        }

        return true;
    }

    @Override
    public String getOwner(final String path) {
        // Don't know of a standard command to get the owner as SID
        return null;
    }

    @Override
    public void setOwner(final String path, final String owner) {
        // Don't know of a standard command to set owner by SID
    }

    public String getCurrentIdentityUser() {
        // Don't know of a way to get the user ignoring impersonation
        return null;
    }

    @Override
    public void grantInheritableFullControl(
        final String path,
        final String user,
        final String copyExplicitRulesFromPath) {
        // This can probably be done with icacls
    }

    @Override
    public void copyExplicitDACLEntries(final String sourcePath, final String targetPath) {
        // This can probably be done with icacls
    }

    @Override
    public void removeExplicitAllowEntries(final String path, final String user) {
        // This can probably be done with icacls
    }

    @Override
    public boolean createSymbolicLink(final String oldpath, final String newpath) {
        return false;
    }

    @Override
    public String[] listMacExtendedAttributes(final String filepath) {
        return null;
    }

    @Override
    public int readMacExtendedAttribute(
        final String filepath,
        final String attribute,
        final byte[] buffer,
        final int size,
        final long position) {
        return -1;
    }

    @Override
    public boolean writeMacExtendedAttribute(
        final String filepath,
        final String attribute,
        final byte[] buffer,
        final int size,
        final long position) {
        return false;
    }

    @Override
    public byte[] getMacExtendedAttribute(final String filepath, final String attribute) {
        return null;
    }

    @Override
    public boolean setMacExtendedAttribute(final String filepath, final String attribute, final byte[] value) {
        return false;
    }
}
