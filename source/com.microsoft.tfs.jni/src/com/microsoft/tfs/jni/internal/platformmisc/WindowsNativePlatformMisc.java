package com.microsoft.tfs.jni.internal.platformmisc;

import com.microsoft.tfs.jni.PlatformMisc;
import com.microsoft.tfs.jni.internal.winapi.Advapi32;
import com.microsoft.tfs.jni.internal.winapi.Kernel32;
import com.sun.jna.Memory;
import com.sun.jna.WString;
import com.sun.jna.platform.win32.Kernel32Util;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;

public class WindowsNativePlatformMisc implements PlatformMisc {

    private final Kernel32 kernel32 = Kernel32.INSTANCE;
    private final Advapi32 advapi32 = Advapi32.INSTANCE;

    @Override
    public String getHomeDirectory(String username) {
        return null;
    }

    @Override
    public boolean changeCurrentDirectory(String directory) {
        return kernel32.SetCurrentDirectoryW(new WString(directory));
    }

    @Override
    public int getDefaultCodePage() {
        return kernel32.GetACP().intValue();
    }

    @Override
    public String getComputerName() {
        Memory buffer = new Memory(Kernel32.MAX_COMPUTERNAME_LENGTH + 1);
        IntByReference size = new IntByReference((int) buffer.size());
        if (!kernel32.GetComputerNameW(buffer, size)) {
            return null;
        }

        return buffer.getWideString(0L);
    }

    @Override
    public String getEnvironmentVariable(String name) {
        return System.getenv(name);
    }

    @Override
    public String expandEnvironmentString(String value) {
        Memory buffer = new Memory(value.length() * 2 + 2); // * 2 because of UTF-16, + 2 for terminating zero
        WinDef.DWORD newSize = kernel32.ExpandEnvironmentStringsW(
            new WString(value),
            buffer,
            new WinDef.DWORD(buffer.size()));

        if (buffer.size() < newSize.longValue()) {
            buffer = new Memory(newSize.longValue());
            newSize = kernel32.ExpandEnvironmentStringsW(
                new WString(value),
                buffer,
                new WinDef.DWORD(buffer.size()));
            if (newSize.intValue() == 0) {
                // Some weird error (maybe syntax?).
                return null;
            }
        }

        return buffer.getWideString(0L);
    }

    @Override
    public String getCurrentIdentityUser() {
        WinNT.HANDLE currentProcess = kernel32.GetCurrentProcess(); // doesn't need to be closed
        WinNT.HANDLEByReference processTokenHandle = new WinNT.HANDLEByReference();

        if (!kernel32.OpenProcessToken(currentProcess, new WinDef.DWORD(Kernel32.TOKEN_QUERY), processTokenHandle)) {
            throw new RuntimeException(
                "Error getting the current process' token: " + Kernel32Util.getLastErrorMessage());
        }

        try {
            IntByReference ownerTokenSize = new IntByReference();
            if (!advapi32.GetTokenInformation(
                processTokenHandle.getValue(),
                WinNT.TOKEN_INFORMATION_CLASS.TokenOwner,
                null,
                0,
                ownerTokenSize)) {
                int lastError = kernel32.GetLastError();
                if (lastError != Kernel32.ERROR_INSUFFICIENT_BUFFER) {
                    throw new RuntimeException(
                        "Error getting token information size: " + Kernel32Util.getLastErrorMessage());
                }
            }

            Memory ownerToken = new Memory(ownerTokenSize.getValue());
            ownerToken.clear();
            WinNT.TOKEN_OWNER tokenOwner = new WinNT.TOKEN_OWNER(ownerToken);

            if (!advapi32.GetTokenInformation(
                processTokenHandle.getValue(),
                WinNT.TOKEN_INFORMATION_CLASS.TokenOwner,
                tokenOwner,
                ownerTokenSize.getValue(),
                ownerTokenSize)) {
                throw new RuntimeException("Error getting token information: " + Kernel32Util.getLastErrorMessage());
            }

            tokenOwner.read();
            PointerByReference ownerSidString = new PointerByReference();
            if (!advapi32.ConvertSidToStringSidW(tokenOwner.Owner, ownerSidString)) {
                throw new RuntimeException(
                    "Error converting SID to string SID: " + Kernel32Util.getLastErrorMessage());
            }

            try {
                return ownerSidString.getValue().getWideString(0L);
            } finally {
                kernel32.LocalFree(ownerSidString.getValue());
            }
        } finally {
            kernel32.CloseHandle(processTokenHandle.getValue());
        }
    }

    @Override
    public String getWellKnownSID(int wellKnownSIDType, String domainSIDString) {
        WinNT.PSIDByReference domainSid = new WinNT.PSIDByReference();
        if (domainSIDString != null) {
            if (!advapi32.ConvertStringSidToSidW(new WString(domainSIDString), domainSid)) {
                throw new RuntimeException("Error converting SID " + domainSIDString + "to SID: " + Kernel32Util.getLastErrorMessage());
            }
        }

        try {
            WinNT.PSID wellKnownSid = new WinNT.PSID(Advapi32.SECURITY_MAX_SID_SIZE);
            IntByReference wellKnownSidSize = new IntByReference(Advapi32.SECURITY_MAX_SID_SIZE);
            if (!advapi32.CreateWellKnownSid(wellKnownSIDType, domainSid.getValue(), wellKnownSid, wellKnownSidSize)) {
                throw new RuntimeException("Error retrieving a well known SID for type "
                    + wellKnownSIDType
                    + ", domain "
                    + domainSIDString
                    + ": "
                    + Kernel32Util.getLastErrorMessage());
            }

            PointerByReference wellKnownSidString = new PointerByReference();
            if (!advapi32.ConvertSidToStringSidW(wellKnownSid, wellKnownSidString)) {
                throw new RuntimeException(
                    "Error converting SID to string SID: " + Kernel32Util.getLastErrorMessage());
            }

            try {
                return wellKnownSidString.getValue().getWideString(0L);
            } finally {
                kernel32.LocalFree(wellKnownSidString.getValue());
            }
        } finally {
            if (domainSid.getValue() != null)
                kernel32.LocalFree(domainSid.getValue().getPointer());
        }
    }
}
