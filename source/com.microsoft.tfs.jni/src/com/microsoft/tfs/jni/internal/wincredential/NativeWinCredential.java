// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.jni.internal.wincredential;

import com.microsoft.tfs.jni.WinCredential;
import com.microsoft.tfs.jni.internal.LibraryNames;
import com.microsoft.tfs.jni.internal.wincredential.winapi.Advapi32;
import com.microsoft.tfs.jni.internal.wincredential.winapi.CREDENTIALW;
import com.microsoft.tfs.jni.loader.NativeLoader;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.WString;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.ptr.PointerByReference;

public class NativeWinCredential {

    public boolean storeCredential(final WinCredential credentialToStore) {
        CREDENTIALW credential = new CREDENTIALW();
        credential.Flags = new WinDef.DWORD(0L);
        credential.Type = new WinDef.DWORD(Advapi32.CRED_TYPE_GENERIC);
        credential.TargetName = new WString(credentialToStore.getServerUri());

        String password = credentialToStore.getPassword();
        Memory passwordMemory = new Memory(password.length());
        try {
            passwordMemory.setString(0, password, "UTF-16");
            credential.CredentialBlobSize = new WinDef.DWORD(passwordMemory.size());
            credential.CredentialBlob = passwordMemory.getPointer(0);

            credential.Persist = new WinDef.DWORD(Advapi32.CRED_PERSIST_LOCAL_MACHINE);
            credential.UserName = new WString(credentialToStore.getAccountName());

            return Advapi32.INSTANCE.CredWriteW(credential, new WinDef.DWORD(0L));
        } finally {
            passwordMemory.clear();
        }
    }

    public WinCredential findCredential(final WinCredential credential) {
        String targetName = credential.getServerUri();

        Advapi32 advapi32 = Advapi32.INSTANCE;

        PointerByReference credentialPtrPtr = new PointerByReference();
        if (advapi32.CredReadW(
            new WString(targetName),
            new WinDef.DWORD(Advapi32.CRED_TYPE_GENERIC),
            new WinDef.DWORD(0),
            credentialPtrPtr)) {
            try {
                CREDENTIALW readCredential = new CREDENTIALW(credentialPtrPtr.getValue());
                String accountName = readCredential.UserName == null ? null : readCredential.UserName.toString();
                char[] passwordArray = readCredential.CredentialBlob == null ? null : readCredential.CredentialBlob.getCharArray(0, readCredential.CredentialBlobSize.intValue());
                String password = passwordArray == null ? null : new String(passwordArray);

                return new WinCredential(targetName, accountName, password);
            } finally {
                advapi32.CredFree(credentialPtrPtr.getValue());
            }
        }

        return null;
    }

    public boolean eraseCredential(final WinCredential cred) {
        return Advapi32.INSTANCE.CredDeleteW(
            new WString(cred.getServerUri()),
            new WinDef.DWORD(Advapi32.CRED_TYPE_GENERIC),
            new WinDef.DWORD(0L));
    }
}