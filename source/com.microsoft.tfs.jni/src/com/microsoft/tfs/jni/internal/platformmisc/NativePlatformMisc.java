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

        if (nativeChangeCurrentDirectory(directory) == 0) {
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

        return nativeGetDefaultCodePage();
    }

    @Override
    public String getComputerName() {
        final String name = nativeGetComputerName();

        if (name == null || name.length() == 0) {
            return null;
        }

        return name;
    }

    @Override
    public String getEnvironmentVariable(final String name) {
        Check.notNullOrEmpty(name, "name"); //$NON-NLS-1$

        /*
         * On Unix, nativeGetEnvironmentVariable() calls getenv(). The ISO C
         * standard (as well as Open Group Base Standard IEEE 1003.1-2001, which
         * defers to ISO C) says getenv() may return a pointer to memory which
         * may be written to by subsequent or concurrent calls to putenv(). In
         * short, getenv() and putenv() are notrequired to be thread-safe. On
         * some platforms, like Solaris, they have always been safe. On others,
         * they have been unsafe at times.
         *
         * Since Java's standard libraries do not implement putenv(), and
         * System.getenv() caches the environment (on Sun's implementation, at
         * least), a Java or JNI putenv() implementation wouldn't work with many
         * existing System.getenv() implementations.
         *
         * For these reasons we assume that putenv() won't ever be called in our
         * Java processes by any thread, so we don't use a lock when calling the
         * native code that calls getenv().
         *
         * http://www.opengroup.org/onlinepubs/009695399/functions/getenv.html
         */

        /*
         * On Windows, the native implementation is thread-safe.
         */

        return nativeGetEnvironmentVariable(name);
    }

    @Override
    public String expandEnvironmentString(final String value) {
        Check.notNull(value, "value"); //$NON-NLS-1$

        if (Platform.isCurrentPlatform(Platform.WINDOWS) == false) {
            return value;
        }

        return nativeExpandEnvironmentString(value);
    }

    @Override
    public String getCurrentIdentityUser() {
        return nativeGetCurrentIdentityUser();
    }

    @Override
    public String getWellKnownSID(final int wellKnownSIDType, final String domainSIDString) {
        return nativeGetWellKnownSID(wellKnownSIDType, domainSIDString);
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
