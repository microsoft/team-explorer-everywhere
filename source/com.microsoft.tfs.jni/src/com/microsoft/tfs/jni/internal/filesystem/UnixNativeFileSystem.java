package com.microsoft.tfs.jni.internal.filesystem;

import com.microsoft.tfs.jni.FileSystem;
import com.microsoft.tfs.jni.FileSystemAttributes;
import com.microsoft.tfs.jni.FileSystemTime;
import com.microsoft.tfs.jni.internal.unix.LibC;
import com.sun.jna.platform.linux.XAttr;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.microsoft.tfs.jni.internal.unix.LibC.*;

public class UnixNativeFileSystem implements FileSystem {

    protected final LibC libC;

    public UnixNativeFileSystem(LibC libC) {
        this.libC = libC;
    }

    private FileSystemTime toFileSystemTime(FileTime fileTime) {
        long nanos = fileTime.to(TimeUnit.NANOSECONDS);
        long seconds = fileTime.toMillis() / 1000L;
        long excessNanos = nanos - seconds * 1000_000_000L;
        return new FileSystemTime(seconds, excessNanos);
    }

    @Override
    public FileSystemAttributes getAttributes(String filepath) {
        Path path = Paths.get(filepath);
        if (!Files.exists(path, LinkOption.NOFOLLOW_LINKS)) {
            return new FileSystemAttributes(
                false,
                null,
                0L,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false);
        }

        PosixFileAttributes attributes = null;
        try {
            attributes = Files.readAttributes(path, PosixFileAttributes.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Set<PosixFilePermission> permissions = attributes.permissions();
        FileTime lastModifiedTime = attributes.lastModifiedTime();
        boolean isSymbolicLink;

        try {
            PosixFileAttributes symlinkAttributes = Files.readAttributes(path, PosixFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
            isSymbolicLink = symlinkAttributes.isSymbolicLink();
            if (isSymbolicLink)
                lastModifiedTime = symlinkAttributes.lastModifiedTime();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return new FileSystemAttributes(
            true,
            toFileSystemTime(lastModifiedTime),
            attributes.size(),
            !permissions.contains(PosixFilePermission.OWNER_WRITE),
            !permissions.contains(PosixFilePermission.GROUP_READ)
                && !permissions.contains(PosixFilePermission.GROUP_WRITE)
                && !permissions.contains(PosixFilePermission.GROUP_EXECUTE)
                && !permissions.contains(PosixFilePermission.OTHERS_READ)
                && !permissions.contains(PosixFilePermission.OTHERS_WRITE)
                && !permissions.contains(PosixFilePermission.OTHERS_EXECUTE),
            permissions.contains(PosixFilePermission.GROUP_WRITE)
                && permissions.contains(PosixFilePermission.OTHERS_WRITE),
            false,
            false,
            attributes.isDirectory(),
            false,
            false,
            permissions.contains(PosixFilePermission.OWNER_EXECUTE),
            isSymbolicLink);
    }

    protected int getFileMode(PosixFileAttributes attributes) {
        int result = 0;
        for (PosixFilePermission permission : attributes.permissions()) {
            switch (permission) {
                case OWNER_READ:
                    result |= LibC.S_IRUSR;
                    break;
                case OWNER_WRITE:
                    result |= S_IWUSR;
                    break;
                case OWNER_EXECUTE:
                    result |= LibC.S_IXUSR;
                    break;
                case GROUP_READ:
                    result |= LibC.S_IRGRP;
                    break;
                case GROUP_WRITE:
                    result |= S_IWGRP;
                    break;
                case GROUP_EXECUTE:
                    result |= LibC.S_IXGRP;
                    break;
                case OTHERS_READ:
                    result |= LibC.S_IROTH;
                    break;
                case OTHERS_WRITE:
                    result |= S_IWOTH;
                    break;
                case OTHERS_EXECUTE:
                    result |= LibC.S_IXOTH;
                    break;
            }
        }

        return result;
    }

    @Override
    public boolean setAttributes(String filepath, FileSystemAttributes attributes) {
        PosixFileAttributes currentAttributes;
        try {
            currentAttributes = Files.readAttributes(Paths.get(filepath), PosixFileAttributes.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        int flags = getFileMode(currentAttributes);
        int umask = libC.umask(0);
        int newMode = flags;

        if (attributes.isPublicWritable()) {
            /* If we want a publicly writable file, override the umask with 0. */
            umask = 0;
        } else {
            /* Remove group and other write bits. */
            newMode &= ~(S_IWGRP | S_IWOTH);
        }

        if (attributes.isReadOnly()) {
            /* Remove all write bits. */
            newMode &= ~(S_IWUSR | S_IWGRP | S_IWOTH);
        } else {
            /* Flip on some read and write bits, allowing only what passes umask. */
            newMode |= ((S_IRUSR | S_IWUSR | S_IRGRP | S_IWGRP | S_IROTH | S_IWOTH) & ~umask);
        }

        if (attributes.isExecutable()) {
            /* Flip on some read and executable bits, allowing only what passes umask. */
            newMode |= ((S_IRUSR | S_IXUSR | S_IRGRP | S_IXGRP | S_IROTH | S_IXOTH) & ~umask);
        } else {
            /* Remove all execute bits. */
            newMode &= ~(S_IXUSR | S_IXGRP | S_IXOTH);
        }

        if (attributes.isOwnerOnly()) {
            /* Remove all bits for group and others, regardless of umask or public writable. */
            newMode &= ~(S_IRGRP | S_IWGRP | S_IXGRP | S_IROTH | S_IWOTH | S_IXOTH);
        }

        return libC.chmod(filepath, newMode) == 0;
    }

    @Override
    public String getOwner(String path) {
        throw new RuntimeException("Platform not supported");
    }

    @Override
    public void setOwner(String path, String user) {
        throw new RuntimeException("Platform not supported");
    }

    @Override
    public void grantInheritableFullControl(String path, String user, String copyExplicitRulesFromPath) {
        throw new RuntimeException("Platform not supported");
    }

    @Override
    public void copyExplicitDACLEntries(String sourcePath, String targetPath) {
        throw new RuntimeException("Platform not supported");
    }

    @Override
    public void removeExplicitAllowEntries(String path, String user) {
        throw new RuntimeException("Platform not supported");
    }

    @Override
    public boolean createSymbolicLink(String oldpath, String newpath) {
        return libC.symlink(oldpath, newpath) == 0;
    }

    @Override
    public String[] listMacExtendedAttributes(String filepath) {
        throw new RuntimeException("Platform not supported");
    }

    @Override
    public int readMacExtendedAttribute(String filepath, String attribute, byte[] buffer, int size, long position) {
        throw new RuntimeException("Platform not supported");
    }

    @Override
    public boolean writeMacExtendedAttribute(
        String filepath,
        String attribute,
        byte[] buffer,
        int size,
        long position) {
        throw new RuntimeException("Platform not supported");
    }

    @Override
    public byte[] getMacExtendedAttribute(String filepath, String attribute) {
        throw new RuntimeException("Platform not supported");
    }

    @Override
    public boolean setMacExtendedAttribute(String filepath, String attribute, byte[] value) {
        throw new RuntimeException("Platform not supported");
    }

    public String getSymbolicLink(String path) {
        XAttr.size_t size = libC.readlink(path, null, new XAttr.size_t(0L));
        byte[] buf = new byte[size.intValue() + 1];
        XAttr.size_t result = libC.readlink(path, buf, new XAttr.size_t(buf.length));
        if (result.intValue() < 0 || result.intValue() >= buf.length) {
            return null;
        }

        return new String(buf, Charset.defaultCharset());
    }
}
