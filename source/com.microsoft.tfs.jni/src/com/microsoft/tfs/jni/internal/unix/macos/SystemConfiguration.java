package com.microsoft.tfs.jni.internal.unix.macos;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.platform.mac.CoreFoundation;

public interface SystemConfiguration extends Library {
    SystemConfiguration INSTANCE = Native.load("SystemConfiguration", SystemConfiguration.class);

    SCPreferencesRef SCPreferencesCreate(
        CoreFoundation.CFAllocatorRef allocator,
        CoreFoundation.CFStringRef name,
        CoreFoundation.CFStringRef prefsID);

    CoreFoundation.CFStringRef SCPreferencesGetHostName(SCPreferencesRef preferences);
    CoreFoundation.CFStringRef SCDynamicStoreCopyLocalHostName(SCDynamicStoreRef store);
}
