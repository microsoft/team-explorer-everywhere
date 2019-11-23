// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.jni.internal.platformmisc;

import java.io.File;
import java.io.IOException;

import com.microsoft.tfs.jni.PlatformMisc;
import com.microsoft.tfs.jni.internal.LibraryNames;
import com.microsoft.tfs.jni.loader.NativeLoader;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.Platform;

/**
 * An implementation of the {@link PlatformMisc} interface that uses native
 * methods.
 *
 * @threadsafety thread-safe
 */
public class NativePlatformMisc implements PlatformMisc {

    private final PlatformMisc backend;

    public NativePlatformMisc() {
        if (Platform.isCurrentPlatform(Platform.WINDOWS))
            backend = new WindowsNativePlatformMisc();
        else if (Platform.isCurrentPlatform(Platform.MAC_OS_X))
            backend = new MacOsNativePlatformMisc();
        else
            backend = new LinuxNativePlatformMisc();
    }

    @Override
    public String getHomeDirectory(final String username) {
        Check.notNull(username, "username"); //$NON-NLS-1$

        if (Platform.isCurrentPlatform(Platform.GENERIC_UNIX) == false) {
            return null;
        }

        return backend.getHomeDirectory(username);
    }

    @Override
    public boolean changeCurrentDirectory(final String directory) {
        Check.notNull(directory, "directory"); //$NON-NLS-1$

        if (backend.changeCurrentDirectory(directory)) {
            /*
             * We must set this variable for Java classes to have any idea that
             * the paths have changed. Canonical path is much nicer to
             * view/debug, so try that first.
             */
            try {
                System.setProperty("user.dir", new File(directory).getCanonicalPath()); //$NON-NLS-1$
            } catch (final IOException e) {
                System.setProperty("user.dir", new File(directory).getAbsolutePath()); //$NON-NLS-1$
            }

            return true;
        }

        return false;
    }

    @Override
    public int getDefaultCodePage() {
        if (Platform.isCurrentPlatform(Platform.WINDOWS) == false) {
            return -1;
        }

        return backend.getDefaultCodePage();
    }

    @Override
    public String getComputerName() {
        final String name = backend.getComputerName();

        if (name == null || name.length() == 0) {
            return null;
        }

        return name;
    }

    @Override
    public String getEnvironmentVariable(final String name) {
        Check.notNullOrEmpty(name, "name"); //$NON-NLS-1$

        String value = backend.getEnvironmentVariable(name);
        return value == null || value.length() == 0 ? null : value;
    }

    @Override
    public String expandEnvironmentString(final String value) {
        Check.notNull(value, "value"); //$NON-NLS-1$

        if (Platform.isCurrentPlatform(Platform.WINDOWS) == false) {
            return value;
        }

        return backend.expandEnvironmentString(value);
    }

    @Override
    public String getCurrentIdentityUser() {
        return backend.getCurrentIdentityUser();
    }

    @Override
    public String getWellKnownSID(final int wellKnownSIDType, final String domainSIDString) {
        return backend.getWellKnownSID(wellKnownSIDType, domainSIDString);
    }
}
