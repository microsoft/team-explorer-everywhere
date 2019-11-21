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

    private final PlatformMisc backend = Platform.isCurrentPlatform(Platform.WINDOWS)
        ? new WindowsNativePlatformMisc()
        : null;

    /**
     * This static initializer is a "best-effort" native code loader (no
     * exceptions thrown for normal load failures).
     *
     * Apps with multiple classloaders (like Eclipse) can run this initializer
     * more than once in a single JVM OS process, and on some platforms
     * (Windows) the native libraries will fail to load the second time, because
     * they're already loaded. This failure can be ignored because the native
     * code will execute fine.
     */
    static {
        NativeLoader.loadLibraryAndLogError(LibraryNames.MISC_LIBRARY_NAME);
    }

    public NativePlatformMisc() {
    }

    @Override
    public String getHomeDirectory(final String username) {
        Check.notNull(username, "username"); //$NON-NLS-1$

        if (Platform.isCurrentPlatform(Platform.GENERIC_UNIX) == false) {
            return null;
        }

        return nativeGetHomeDirectory(username);
    }

    @Override
    public boolean changeCurrentDirectory(final String directory) {
        Check.notNull(directory, "directory"); //$NON-NLS-1$

        boolean success;
        if (backend != null)
            success = backend.changeCurrentDirectory(directory);
        else
            success = nativeChangeCurrentDirectory(directory) == 0;

        if (success) {
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
        }

        return success;
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
        final String name = backend == null ? nativeGetComputerName() : backend.getComputerName();

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
        return backend == null ? nativeGetCurrentIdentityUser() : backend.getCurrentIdentityUser();
    }

    @Override
    public String getWellKnownSID(final int wellKnownSIDType, final String domainSIDString) {
        return backend == null
            ? nativeGetWellKnownSID(wellKnownSIDType, domainSIDString)
            : backend.getWellKnownSID(wellKnownSIDType, domainSIDString);
    }

    private static native int nativeChangeCurrentDirectory(String directory);

    private static native String nativeGetComputerName();

    private static native String nativeGetEnvironmentVariable(String name);

    // WARNING: Following only available on Windows.

    private static native int nativeGetDefaultCodePage();

    private static native String nativeGetCurrentIdentityUser();

    private static native String nativeExpandEnvironmentString(String value);

    private static native String nativeGetWellKnownSID(int wellKnownSIDType, String domainSIDString);

    // WARNING: Following only available on Unix.

    private static native String nativeGetHomeDirectory(String username);
}
