// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.jni;

import java.text.MessageFormat;

import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.Platform;

/**
 * Represents the filesystem attributes for a given folder or directory,
 * including modification time, file size and permissions.
 *
 * @threadsafety thread safe
 */
public class FileSystemAttributes {
    private final boolean exists;
    private final FileSystemTime modificationTime;
    private final long size;
    private boolean readOnly;
    private boolean ownerOnly; /* implies 0700 or 0600 */
    private boolean publicWritable; /* implies 0777 or 0666 */
    private boolean hidden;
    private boolean system;
    private final boolean directory;
    private boolean archive;
    private boolean notContentIndexed;
    private boolean executable;
    private final boolean symbolicLink;

    /**
     * Creates a new {@link FileSystemAttributes} with default values.
     *
     * @equivalence this(false, new FileSystemTime(0), -1, false, false, false,
     *              false, false, false, false, false, false)
     */
    public FileSystemAttributes() {
        this(false, new FileSystemTime(0), -1, false, false, false, false, false, false, false, false, false, false);
    }

    /**
     * Creates a new {@link FileSystemAttributes}.
     *
     * @param exists
     *        <code>true</code> if the file exists, <code>false</code> otherwise
     * @param modificationTime
     *        The modification time of the file (may not be <code>null</code> if
     *        the file exists)
     * @param size
     *        The size of the file in bytes
     * @param readOnly
     *        <code>true</code> if the file is read-only, <code>false</code>
     *        otherwise
     * @param ownerOnly
     *        <code>true</code> if the file is only accessible by the owner (ie,
     *        is mode 0700 or 0600), <code>false</code> otherwise
     * @param publicWritable
     *        <code>true</code> if the file is writable by the public (ie, the
     *        group or other write bit is set), <code>false</code> otherwise
     * @param hidden
     *        <code>true</code> if the file is hidden, <code>false</code>
     *        otherwise (Windows only)
     * @param system
     *        <code>true</code> if the file is a system file, <code>false</code>
     *        otherwise (Windows only)
     * @param directory
     *        <code>true</code> if the filesystem object is a directory,
     *        <code>false</code> otherwise
     * @param archive
     *        <code>true</code> if the file has the archive bit set,
     *        <code>false</code> otherwise (Windows only)
     * @param notContentIndexed
     *        <code>true</code> if the file is not to be content indexed,
     *        <code>false</code> otherwise
     * @param executable
     *        <code>true</code> if the file is executable, <code>false</code>
     *        otherwise (Unix only)
     * @param symbolicLink
     *        <code>true</code> if the file is, <code>false</code> otherwise
     *        (Unix only)
     */
    public FileSystemAttributes(
        final boolean exists,
        final FileSystemTime modificationTime,
        final long size,
        final boolean readOnly,
        final boolean ownerOnly,
        final boolean publicWritable,
        final boolean hidden,
        final boolean system,
        final boolean directory,
        final boolean archive,
        final boolean notContentIndexed,
        final boolean executable,
        final boolean symbolicLink) {
        if (exists) {
            Check.notNull(modificationTime, "modificationTime"); //$NON-NLS-1$
        }

        this.exists = exists;
        this.modificationTime = modificationTime;
        this.size = size;
        this.readOnly = readOnly;
        this.ownerOnly = ownerOnly;
        this.publicWritable = publicWritable;
        this.hidden = hidden;
        this.system = system;
        this.directory = directory;
        this.archive = archive;
        this.notContentIndexed = notContentIndexed;
        this.executable = executable;
        this.symbolicLink = symbolicLink;
    }

    /**
     * Returns whether this file or directory exists
     *
     * @return <code>true</code> if this file or directory exists, false
     *         otherwise
     */
    public boolean exists() {
        return exists;
    }

    /**
     * Returns the last modified time for this file.
     *
     * @return The {@link FileSystemTime} this file was last modified, or
     *         <code>null</code> if the file does not exist
     */
    public FileSystemTime getModificationTime() {
        return modificationTime;
    }

    /**
     * Returns the size (in bytes) of this file
     *
     * @return The size (in bytes) of this file
     */
    public long getSize() {
        return size;
    }

    /**
     * Returns whether this file is read-only
     *
     * @return <code>true</code> if the file is read-only, <code>false</code>
     *         otherwise
     */
    public boolean isReadOnly() {
        return readOnly;
    }

    /**
     * Sets the file attribute to be read-only.
     *
     * @param readOnly
     *        <code>true</code> if the file is to be made read-only,
     *        <code>false</code> otherwise
     */
    public void setReadOnly(final boolean readOnly) {
        this.readOnly = readOnly;
    }

    /**
     * Returns whether this file is "owner-only" (meaning that the file is
     * created with 0700 mode on Unix)
     *
     * This has no effect on Windows.
     *
     * @return <code>true</code> if the file is "owner-only", <code>false</code>
     *         otherwise
     */
    public boolean isOwnerOnly() {
        return ownerOnly;
    }

    /**
     * Sets the file attribute to be "owner-only" (meaning that the file is
     * created with 0700 mode on Unix).
     *
     * This has no effect on Windows.
     *
     * @param ownerOnly
     *        <code>true</code> if the file is to be made "owner-only",
     *        <code>false</code> otherwise
     */
    public void setOwnerOnly(final boolean ownerOnly) {
        this.ownerOnly = ownerOnly;
    }

    /**
     * Returns whether this file is "publicly writable" (meaning that the file
     * is readable by group or other users).
     *
     * This has no effect on Windows.
     *
     * @return <code>true</code> if the file is "publicly writable",
     *         <code>false</code> otherwise
     */
    public boolean isPublicWritable() {
        return publicWritable;
    }

    /**
     * Sets the file attribute to be "publicly writable" (meaning that the file
     * is created with 0777 or 0666 mode on Unix.)
     *
     * This has no effect on Windows.
     *
     * @param publicWritable
     *        <code>true</code> if the file is to be made "publicly writable",
     *        <code>false</code> otherwise
     */
    public void setPublicWritable(final boolean publicWritable) {
        this.publicWritable = publicWritable;
    }

    /**
     * Returns whether this file is hidden. If the operating system does not
     * have a concept of hidden files (e.g., Unix), returns <code>false</code>.
     *
     * @return <code>true</code> if the file is hidden, <code>false</code>
     *         otherwise
     */
    public boolean isHidden() {
        return hidden;
    }

    /**
     * Sets the file attribute to be hidden.
     *
     * @param hidden
     *        <code>true</code> if the file is to be made hidden,
     *        <code>false</code> otherwise
     */
    public void setHidden(final boolean hidden) {
        this.hidden = hidden;
    }

    /**
     * Returns whether this file is a system file. If the operating system does
     * not have a concept of system files (e.g., Unix), returns
     * <code>false</code>.
     *
     * @return <code>true</code> if the file is a system file,
     *         <code>false</code> otherwise
     */
    public boolean isSystem() {
        return system;
    }

    /**
     * Sets the file attribute to be a system file.
     *
     * @param system
     *        <code>true</code> if the file is to be made a system file,
     *        <code>false</code> otherwise
     */
    public void setSystem(final boolean system) {
        this.system = system;
    }

    /**
     * Returns whether this filesystem object is a directory
     *
     * @return <code>true</code> if the file is directory, <code>false</code>
     *         otherwise
     */
    public boolean isDirectory() {
        return directory && !isSymbolicLink();
    }

    /**
     * Returns whether this file has the archive bit set. If the operating
     * system does not have a concept of archive files (e.g., Unix), returns
     * <code>false</code>.
     *
     * @return <code>true</code> if the file has the archive bit set,
     *         <code>false</code> otherwise
     */
    public boolean isArchive() {
        return archive;
    }

    /**
     * Sets the file attribute to have the archive bit set.
     *
     * @param archive
     *        <code>true</code> if the file is to have the archive bit set,
     *        <code>false</code> otherwise
     */
    public void setArchive(final boolean archive) {
        this.archive = archive;
    }

    /**
     * Returns whether this file is to be content indexed.
     *
     * @return <code>true</code> if the file is not to be content indexed,
     *         <code>false</code> otherwise
     */
    public boolean isNotContentIndexed() {
        return notContentIndexed;
    }

    /**
     * Sets the file attribute such that the file is not to be content indexed.
     *
     * @param notContentIndexed
     *        <code>true</code> if the file is not to be content indexed,
     *        <code>false</code> otherwise
     */
    public void setNotContentIndexed(final boolean notContentIndexed) {
        this.notContentIndexed = notContentIndexed;
    }

    /**
     * Returns whether this file is executable. If the operating system does not
     * have a concept of executable files (e.g., Windows), returns
     * <code>false</code>.
     *
     * @return <code>true</code> if the file is executable, <code>false</code>
     *         otherwise
     */
    public boolean isExecutable() {
        if (!Platform.isCurrentPlatform(Platform.GENERIC_UNIX)) {
            return false;
        }
        return executable;
    }

    /**
     * Sets the file attribute to be executable.
     *
     * @param executable
     *        <code>true</code> if the file is to be made executable,
     *        <code>false</code> otherwise
     */
    public void setExecutable(final boolean executable) {
        this.executable = executable;
    }

    /**
     * Returns whether this file is a symbolic link. If the operating system
     * does not have a concept of symbolic links (e.g., Windows), returns
     * <code>false</code>.
     *
     * @return <code>true</code> if the file is a symbolic link,
     *         <code>false</code> otherwise
     */
    public boolean isSymbolicLink() {
        if (!Platform.isCurrentPlatform(Platform.GENERIC_UNIX)) {
            return false;
        }
        return symbolicLink;
    }

    @Override
    public String toString() {
        return MessageFormat.format(
            "FileSystemAttributes [exists={0}, modificationTime={1}, size={2}, readOnly={3}, ownerOnly={4}, publicWritable={5}, hidden={6}, system={7}, directory={8}, archive={9}, executable={10}, symbolicLink={11}]", //$NON-NLS-1$
            exists,
            modificationTime,
            size,
            readOnly,
            ownerOnly,
            publicWritable,
            hidden,
            system,
            directory,
            archive,
            executable,
            symbolicLink);
    }
}
