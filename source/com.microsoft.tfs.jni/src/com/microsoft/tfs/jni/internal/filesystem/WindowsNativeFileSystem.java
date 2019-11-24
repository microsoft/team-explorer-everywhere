package com.microsoft.tfs.jni.internal.filesystem;

import com.microsoft.tfs.jni.FileSystem;
import com.microsoft.tfs.jni.FileSystemAttributes;
import com.microsoft.tfs.jni.FileSystemTime;
import com.microsoft.tfs.jni.internal.winapi.*;
import com.microsoft.tfs.util.Check;
import com.sun.jna.WString;
import com.sun.jna.platform.win32.AccCtrl;
import com.sun.jna.platform.win32.Kernel32Util;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.ptr.PointerByReference;

public class WindowsNativeFileSystem implements FileSystem {
    private static final Kernel32 kernel32 = Kernel32.INSTANCE;
    private static final Advapi32 advapi32 = Advapi32.INSTANCE;

    @Override
    public FileSystemAttributes getAttributes(String filepath) {
        if (filepath == null)
            return null;

        WIN32_FILE_ATTRIBUTE_DATA attributes = new WIN32_FILE_ATTRIBUTE_DATA();
        if (!kernel32.GetFileAttributesExW(new WString(filepath), Kernel32.GetFileExInfoStandard, attributes)) {
            int error = kernel32.GetLastError();
            switch (error) {
                case WinNT.ERROR_TOO_MANY_OPEN_FILES:
                case WinNT.ERROR_READ_FAULT:
                case WinNT.ERROR_SHARING_VIOLATION:
                case WinNT.ERROR_LOCK_VIOLATION:
                    throw new RuntimeException(Kernel32Util.getLastErrorMessage());
                default:
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
        }

        WinDef.DWORDLONG modificationTimeTicks = attributes.ftLastWriteTime.toDWordLong();
        long modificationTimeSeconds = modificationTimeTicks.longValue() / 10000000L - 11644473600L;
        long modificationTimeNanoseconds = modificationTimeTicks.longValue() % 10000000L;

        long fileSize = ((attributes.nFileSizeHigh.longValue() & 0xffffffffL) << 32)
            | (attributes.nFileSizeLow.longValue() & 0xffffffffL);
        FileSystemTime time = new FileSystemTime(modificationTimeSeconds, modificationTimeNanoseconds);
        return new FileSystemAttributes(
            true,
            time,
            fileSize,
            (attributes.dwFileAttributes.intValue() & WinNT.FILE_ATTRIBUTE_READONLY) == WinNT.FILE_ATTRIBUTE_READONLY,
            false,
            false,
            (attributes.dwFileAttributes.intValue() & WinNT.FILE_ATTRIBUTE_HIDDEN) == WinNT.FILE_ATTRIBUTE_HIDDEN,
            (attributes.dwFileAttributes.intValue() & WinNT.FILE_ATTRIBUTE_SYSTEM) == WinNT.FILE_ATTRIBUTE_SYSTEM,
            (attributes.dwFileAttributes.intValue() & WinNT.FILE_ATTRIBUTE_DIRECTORY) == WinNT.FILE_ATTRIBUTE_DIRECTORY,
            (attributes.dwFileAttributes.intValue() & WinNT.FILE_ATTRIBUTE_ARCHIVE) == WinNT.FILE_ATTRIBUTE_ARCHIVE,
            (attributes.dwFileAttributes.intValue() & WinNT.FILE_ATTRIBUTE_NOT_CONTENT_INDEXED) == WinNT.FILE_ATTRIBUTE_NOT_CONTENT_INDEXED,
            true,
            false
        );
    }

    @Override
    public boolean setAttributes(String filepath, FileSystemAttributes attributes) {
        if (filepath == null)
            return false;

        int attributeValues = kernel32.GetFileAttributes(filepath);
        if (attributeValues == WinNT.INVALID_FILE_ATTRIBUTES)
            return false;

        attributeValues = attributes.isReadOnly()
            ? attributeValues | WinNT.FILE_ATTRIBUTE_READONLY
            : attributeValues & ~WinNT.FILE_ATTRIBUTE_READONLY;
        attributeValues = attributes.isHidden()
            ? attributeValues | WinNT.FILE_ATTRIBUTE_HIDDEN
            : attributeValues & ~WinNT.FILE_ATTRIBUTE_HIDDEN;
        attributeValues = attributes.isSystem()
            ? attributeValues | WinNT.FILE_ATTRIBUTE_SYSTEM
            : attributeValues & ~WinNT.FILE_ATTRIBUTE_SYSTEM;
        attributeValues = attributes.isArchive()
            ? attributeValues | WinNT.FILE_ATTRIBUTE_ARCHIVE
            : attributeValues & ~WinNT.FILE_ATTRIBUTE_ARCHIVE;
        attributeValues = attributes.isNotContentIndexed()
            ? attributeValues | WinNT.FILE_ATTRIBUTE_NOT_CONTENT_INDEXED
            : attributeValues & ~WinNT.FILE_ATTRIBUTE_NOT_CONTENT_INDEXED;

        return kernel32.SetFileAttributes(filepath, new WinDef.DWORD(attributeValues));
    }

    @Override
    public String getOwner(String path) {
        PointerByReference securityDescriptorPointer = new PointerByReference();
        WinNT.PSIDByReference ownerSid = new WinNT.PSIDByReference();
        if (advapi32.GetNamedSecurityInfoW(
            new WString(path),
            AccCtrl.SE_OBJECT_TYPE.SE_FILE_OBJECT,
            new WinDef.DWORD(WinNT.OWNER_SECURITY_INFORMATION),
            ownerSid,
            null,
            null,
            null,
            securityDescriptorPointer).intValue() != WinNT.ERROR_SUCCESS) {
            throw new RuntimeException("Error getting file security info for " + path);
        }

        try {
            return ownerSid.getValue().getSidString();
        } finally {
            kernel32.LocalFree(securityDescriptorPointer.getValue());
        }
    }

    @Override
    public void setOwner(String path, String user) {
        Check.notNull(path, "path");
        Check.notNull(user, "user");

        WinNT.PSIDByReference ownerSid = new WinNT.PSIDByReference();
        if (!advapi32.ConvertStringSidToSid(user, ownerSid)) {
            throw new RuntimeException("Error converting string SID " + user + " to SID");
        }

        try {
            if (advapi32.SetNamedSecurityInfo(
                path,
                AccCtrl.SE_OBJECT_TYPE.SE_FILE_OBJECT,
                WinNT.OWNER_SECURITY_INFORMATION,
                ownerSid.getValue().getPointer(),
                null,
                null,
                null) != WinNT.ERROR_SUCCESS) {
                throw new RuntimeException("Error setting file security info for " + path);
            }
        } finally {
            kernel32.LocalFree(ownerSid.getValue().getPointer());
        }
    }

    @Override
    public void grantInheritableFullControl(String path, String user, String copyExplicitRulesFromPath) {
        Check.notNull(path, "path");
        Check.notNull(user, "user");

        WinNT.PACLByReference existingDacl = new WinNT.PACLByReference();
        PointerByReference securityDescriptor = new PointerByReference();
        if (copyExplicitRulesFromPath != null) {
            if (advapi32.GetNamedSecurityInfoW(
                new WString(copyExplicitRulesFromPath),
                AccCtrl.SE_OBJECT_TYPE.SE_FILE_OBJECT,
                new WinDef.DWORD(WinNT.DACL_SECURITY_INFORMATION),
                null,
                null,
                existingDacl,
                null,
                securityDescriptor).intValue() != WinNT.ERROR_SUCCESS) {
                throw new RuntimeException("Error getting file security info for " + copyExplicitRulesFromPath);
            }
        }

        try {
            WinNT.PSIDByReference userSid = new WinNT.PSIDByReference();
            if (!advapi32.ConvertStringSidToSid(user, userSid)) {
                throw new RuntimeException("Error converting string SID " + user + " to SID");
            }

            try {
                // Create a new explicit access entry with rights equivalent to .NET's FileSystemRights.FullControl
                // (0x1F01FF; see FileSecurity.cs) and full inheritance.
                EXPLICIT_ACCESSW fullControl = new EXPLICIT_ACCESSW();
                fullControl.grfAccessPermissions = new WinDef.DWORD(0x1F01FF);
                fullControl.grfAccessMode = Advapi32.GRANT_ACCESS;
                fullControl.grfInheritance = new WinDef.DWORD(WinNT.CONTAINER_INHERIT_ACE | WinNT.OBJECT_INHERIT_ACE);
                fullControl.Trustee = new TRUSTEEW();
                fullControl.Trustee.TrusteeForm = Advapi32.TRUSTEE_IS_SID;
                fullControl.Trustee.TrusteeType = Advapi32.TRUSTEE_IS_USER;
                fullControl.Trustee.ptstrName = userSid.getValue().getPointer();

                WinNT.PACLByReference newDacl = new WinNT.PACLByReference();
                if (advapi32.SetEntriesInAclW(
                    new WinDef.ULONG(1L),
                    fullControl,
                    existingDacl.getValue(),
                    newDacl).intValue() != WinNT.ERROR_SUCCESS) {
                    throw new RuntimeException("Error setting entries in ACL");
                }

                try {
                    if (advapi32.SetNamedSecurityInfo(
                        path,
                        AccCtrl.SE_OBJECT_TYPE.SE_FILE_OBJECT,
                        WinNT.DACL_SECURITY_INFORMATION,
                        null,
                        null,
                        newDacl.getValue().getPointer(),
                        null) != WinNT.ERROR_SUCCESS) {
                        throw new RuntimeException("Error setting file security info for " + path);
                    }
                } finally {
                    kernel32.LocalFree(newDacl.getValue().getPointer());
                }
            } finally {
                kernel32.LocalFree(userSid.getValue().getPointer());
            }
        } finally {
            if (securityDescriptor.getValue() != null)
                kernel32.LocalFree(securityDescriptor.getValue());
            // existingDacl points inside securityDescriptor, no need to clean up explicitly
        }
    }

    @Override
    public void copyExplicitDACLEntries(String sourcePath, String targetPath) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public void removeExplicitAllowEntries(String path, String user) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public boolean createSymbolicLink(String oldpath, String newpath) {
        throw new RuntimeException("Not implemented");
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
