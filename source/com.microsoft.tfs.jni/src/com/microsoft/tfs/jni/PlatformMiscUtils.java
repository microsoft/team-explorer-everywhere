// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.jni;

import com.microsoft.tfs.jni.internal.platformmisc.NativePlatformMisc;

public class PlatformMiscUtils implements PlatformMisc {
    private static final PlatformMisc instance = new PlatformMiscUtils();

    /**
     * @return an instance of a {@link PlatformMisc} implementation full of
     *         utility methods that are ready-to-call.
     */
    public static PlatformMisc getInstance() {
        return PlatformMiscUtils.instance;
    }

    private final NativePlatformMisc nativeImpl;

    private PlatformMiscUtils() {
        nativeImpl = new NativePlatformMisc();
    }

    @Override
    public boolean changeCurrentDirectory(final String directory) {
        return nativeImpl.changeCurrentDirectory(directory);
    }

    @Override
    public String getHomeDirectory(final String username) {
        return nativeImpl.getHomeDirectory(username);
    }

    @Override
    public int getDefaultCodePage() {
        return nativeImpl.getDefaultCodePage();
    }

    @Override
    public String getComputerName() {
        return nativeImpl.getComputerName();
    }

    @Override
    public String getEnvironmentVariable(final String name) {
        return nativeImpl.getEnvironmentVariable(name);
    }

    @Override
    public String expandEnvironmentString(final String value) {
        return nativeImpl.expandEnvironmentString(value);
    }

    @Override
    public String getCurrentIdentityUser() {
        return nativeImpl.getCurrentIdentityUser();
    }

    @Override
    public String getWellKnownSID(final int wellKnownSIDType, final String domainSIDString) {
        return nativeImpl.getWellKnownSID(wellKnownSIDType, domainSIDString);
    }
}
