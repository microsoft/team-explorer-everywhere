// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.jni.loader;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.Platform;
import com.microsoft.tfs.util.StringUtil;

/**
 * Loads JNI libraries using an procedure that improves on the default Java
 * {@link System#loadLibrary(String)} search behavior. The new procedure has
 * hooks for providing an explicit library location (SDK users may want this).
 * It also supports loading libraries from classpath resources (they are copied
 * to a temporary location on the filesystem).
 * <p>
 * <b>Loading Method</b>
 * <p>
 * When {@link #loadLibrary(String)} is called, it loads a library in one of the
 * following ways, or throws {@link UnsatisfiedLinkError} if it could not load
 * the library.
 * <p>
 * <ul>
 * <li>If the {@value NativeLoader#NATIVE_LIBRARY_BASE_DIRECTORY_PROPERTY}
 * system property is set and non-empty, it is the only placed searched for the
 * native libraries. The path is the local disk path to the base directory of an
 * OSGI-style operating system and architecture directory tree containing native
 * libraries. Subdirectories inside the base might be named like "linux/ppc",
 * "win32/x86", etc., and the native libraries are located in the deepest
 * directories. This is the hierarchical naming scheme used by Eclipse.
 * <p>
 * If no library is found in the OSGI-style directory tree,
 * {@link UnsatisfiedLinkError} is thrown.</li>
 * <li>If the {@value NativeLoader#NATIVE_LIBRARY_BASE_DIRECTORY_PROPERTY}
 * system property is <em>not</em> set, {@link System#loadLibrary(String)} is
 * invoked on the given library name. This method searches many system-dependent
 * locations for the native library file, including java.library.path,
 * LD_LIBRARY_PATH on Unix platforms, DLL search path on Windows, and possibly
 * more. If {@link System#loadLibrary(String)} is unable to find the library,
 * the next action listed is taken.</li>
 * <li>The classloader that loaded the {@link NativeLoader} class (see the
 * Classloader Note below) is searched for a resource with a path similar to the
 * OSGI-style paths searched when
 * {@value #NATIVE_LIBRARY_BASE_DIRECTORY_PROPERTY} is set. The prefix for this
 * resource path is "/native", then the OSGI-style segments follow. Example
 * resource paths for "fun": "/native/linux/x86_64/libfun.so",
 * "/native/win32/x86/fun.dll". If a resource at this path is found, it is
 * extracted to a temporary location on the filesystem and loaded. If no
 * resource is found, {@link UnsatisfiedLinkError} is thrown.</li>
 * </ul>
 * <p>
 * <b>Classloader Note</b>
 * <p>
 * {@link NativeLoader#loadLibrary(String)} loads native library files using a
 * technique that includes searching the classpath for native library files.
 * When this happens, the classloader that loaded the {@link NativeLoader} class
 * is searched for these resources. {@link NativeLoader#loadLibrary(String)}
 * does not support a configuration where these resources are not visible in the
 * classloader that loaded {@link NativeLoader}, even though they may be visible
 * somewhere else. See the {@link NativeLoader#loadLibrary(String)} Javadoc for
 * details on how libraries are found and loaded.
 * <p>
 * <b>Multiple Library Load Note</b>
 * <p>
 * If a native library is already loaded into the JVM with this classloader,
 * subsequent calls to {@link #loadLibrary(String)} will be ignored. If the
 * library was loaded via a different classloader, {@link #loadLibrary(String)}
 * may throw an exception.
 */
public class NativeLoader {
    private final static Log log = LogFactory.getLog(NativeLoader.class);

    /**
     * The path component that native library resources loaded from classloaders
     * start with. OS, (optionally) architecture, and library name follow.
     */
    public static final String RESOURCE_PATH_PREFIX = "/native"; //$NON-NLS-1$

    /**
     * If this property is set, the given base-directory is searched for a
     * subdirectory with the appropriate OSGI-style operating system and
     * architecture names for this platform, and native libraries are loaded
     * from only this location.
     * <p>
     * For example, if you want to find the "fun-1" native library on disk at
     * "/fun/bar/linux/x86/libfun-1.so", you would use "/fun/bar" as the baes
     * directory property value to allow {@link NativeLoader} to find libraries.
     */
    public static final String NATIVE_LIBRARY_BASE_DIRECTORY_PROPERTY = "com.microsoft.tfs.jni.native.base-directory"; //$NON-NLS-1$

    /*
     * Defined here are architecture strings as reported by the "os.arch" system
     * property. Different JVMs on a platform may use different architecture
     * names, so we list the synonyms here for matching.
     */

    /**
     * Intel's 32-bit x86 architecture.
     * <p>
     * Strings come from:
     * <ul>
     * <li>i386: Sun 32-bit JDK 1.4.2_14, Linux x86</li>
     * <li>i386: Sun 32-bit JDK 1.5.0_09, Linux x86</li>
     * <li>i386: Sun 32-bit JDK 1.6.0_01, Linux x86</li>
     * <li>x86: IBM 32-bit JDK 1.3.1, Linux x86</li>
     * <li>x86: IBM 32-bit JDK 1.4.2 SR8, Linux x86</li>
     * <li>x86: IBM 32-bit JDK 1.5.0 SR5, Linux x86</li>
     * <li>i386: BEA 32-bit JRockit JDK 1.4.2_08, Linux x86</li>
     * <li>i386: BEA 32-bit JRockit JDK 1.5.0_06-b05, Linux x86</li>
     * <li>x86: Sun 32-bit JDK 1.6.0_01-b06, Windows XP SP2 x86</li>
     * <li>i386: Apple 32-bit JDK 1.5.0_07, Mac OS X 10.4</li>
     * </ul>
     */
    private static final String[] X86_SYNONYMS = new String[] {
        "i386", //$NON-NLS-1$
        "x86" //$NON-NLS-1$
    };

    /**
     * AMD's 64-bit architecture based on Intel's x86 instruction set.
     * <p>
     * Strings come from:
     * <ul>
     * <li>amd64: Sun 64-bit Java 1.5.0_09-b03, Linux AMD64</li>
     * <li>amd64: Sun 64-bit Java 1.6.0_01-b06, Linux AMD64</li>
     * <li>amd64: IBM 64-bit Java 1.6.0 jclxa6460-20070509_01, Linux AMD64</li>
     * <li>amd64: BEA 64-bit JRockit JDK 1.5.0_06-b05, Linux AMD64</li>
     * <li>x86_64: Apple 64-bit JDK 1.6.0_07, Mac OS X 10.5</li>
     * </ul>
     */
    private static final String[] AMD64_SYNONYMS = new String[] {
        "amd64", //$NON-NLS-1$
        "x86_64" //$NON-NLS-1$
    };

    /**
     * Sun's 32-bit SPARC (v8 and previous) architecture.
     * <p>
     * Strings come from:
     * <ul>
     * <li>sparc: Sun 32-bit JDK 1.4.2_14-b05, Solaris 8 SPARC 64-bit kernel
     * </li>
     * <li>sparc: Sun 32-bit JDK 1.5.0_08, Solaris 8 SPARC 64-bit kernel</li>
     * <li>sparc: Sun 32-bit JDK 1.6.0_02-b05, Solaris 8 SPARC 64-bit kernel
     * </li>
     * </ul>
     */
    private static final String[] SPARC_32_SYNONYMS = new String[] {
        "sparc" //$NON-NLS-1$
    };

    /**
     * Sun's 64-bit SPARC (v9 or UltraSPARC) architecture.
     * <p>
     * Strings come from:
     * <ul>
     * <li>sparcv9: Sun 64-bit JDK 1.4.2_14-b05, Solaris 8 SPARC 64-bit kernel
     * </li>
     * <li>sparcv9: Sun 64-bit JDK 1.5.0_08, Solaris 8 SPARC 64-bit kernel</li>
     * <li>sparcv9: Sun 64-bit JDK 1.6.0_02-b05, Solaris 8 SPARC 64-bit kernel
     * </li>
     * </ul>
     */
    private static final String[] SPARC_64_SYNONYMS = new String[] {
        "sparcv9" //$NON-NLS-1$
    };

    /**
     * The PowerPC architecture can be implemented as 32-bit or 64-bit. These
     * synonyms are for the 32-bit variant.
     * <p>
     * Strings come from:
     * <ul>
     * <li>ppc: IBM 32-bit JDK 1.5.0 SR2, Linux PowerPC64</li>
     * </ul>
     */
    private static final String[] POWERPC_32_SYNONYMS = new String[] {
        "ppc" //$NON-NLS-1$
    };

    /**
     * The PowerPC architecture can be implemented as 32-bit or 64-bit. These
     * synonyms are for the 64-bit variant.
     * <p>
     * Strings come from:
     * <ul>
     * <li>ppc64: IBM 32-bit JDK 1.4.2 cxppc32142sr1a-20050209, Linux PowerPC64
     * </li>
     * <li>ppc64: Apple 64-bit JDK 1.6.0_07, Mac OS X 10.5</li>
     * </ul>
     */
    private static final String[] POWERPC_64_SYNONYMS = new String[] {
        "ppc64" //$NON-NLS-1$
    };

    /**
     * HP's (now obsolete) PA_RISC architecture. PA_RISC 2.0 is 64-bit, 1.0 and
     * 1.1 are 32-bit and can be loaded by 64-bit architectures so we don't
     * define them separately.
     * <p>
     * Strings come from:
     * <ul>
     * <li>PA_RISC2.0: HP 64-bit JDK 1.4.2.04-040628-20:03-PA_RISC2.0, HP-UX
     * B.11.11</li>
     * <li>PA_RISC2.0: HP 64-bit JDK 1.5.0.04, HP-UX B.11.11</li>
     * </ul>
     */
    private static final String[] PA_RISC_20_SYNONYMS = new String[] {
        "PA_RISC2.0" //$NON-NLS-1$
    };

    /**
     * Intel's Itanium (IA64) architecture running 32-bit ("N" for narrow)
     * applications. HP-UX 11i v2 is the first HP-UX version to support Itanium
     * platforms, and installs a 64-bit kernel but runs mostly 32-bit processes.
     * Java 5 is installed by default on this platform and runs in 32-bit mode
     * by default and with the -d32 flag.
     * <p>
     * Strings come from:
     * <ul>
     * <li>IA64N: HP Java HotSpot(TM) Server VM (build 1.5.0.09
     * jinteg:08.18.07-13:19 IA64, mixed mode), HP-UX B.11.23</li>
     * </ul>
     */
    private static final String[] ITANIUM_32_SYNONYMS = new String[] {
        "IA64N" //$NON-NLS-1$
    };

    /**
     * Intel's Itanium (IA64) architecture running 64-bit ("W" for wide)
     * applications. HP-UX 11i v2 is the first HP-UX version to support Itanium
     * platforms, and installs a 64-bit kernel but runs mostly 32-bit processes.
     * Java 5 is installed by default on this platform and runs in 64-bit mode
     * bif given the -d64 flag.
     * <p>
     * Strings come from:
     * <ul>
     * <li>IA64W: HP Java HotSpot(TM) 64-Bit Server VM (build 1.5.0.09
     * jinteg:08.18.07-15:08 IA64W, mixed mode), HP-UX B.11.23</li>
     * </ul>
     */
    private static final String[] ITANIUM_64_SYNONYMS = new String[] {
        "IA64W" //$NON-NLS-1$
    };

    /**
     * IBM's zSeries mainframe architecture. Supports 31-bit (yes, not 32-bit)
     * and 64-bit applications. Java can be either 31- or 64-bit.
     * <p>
     * Strings come from:
     * <ul>
     * <li></li>
     * <li>390: IBM 31-bit JDK 1.4.2, J2RE 1.4.2 IBM z/OS Persistent Reusable VM
     * build cm142-20061124 (SR7) on z/OS 1.8</li>
     * <li>s390: IBM 31-bit JDK 1.6.0 IBM z/OS Persistent Reusable VM on z/OS
     * 1.8</li>
     * </ul>
     */
    private static final String[] Z_ARCH_SYNONYMS = new String[] {
        "390", //$NON-NLS-1$
        "s390" //$NON-NLS-1$
    };

    /**
     * Experimental support for ARM chipsets such as the Raspberry Pi.
     */
    private static final String[] ARM_32_SYNONYMS = new String[] {
        "arm", //$NON-NLS-1$
        "armv6l" //$NON-NLS-1$
    };

    /**
     * @return the OSGI-style operating system name running this method. Returns
     *         null if the current operating system is unknown or does not map
     *         to an OSGI-style operating system name.
     */
    private static String getOSGIOperatingSystem() {
        /*
         * The Platform class tests JVM system properties for us.
         */

        if (Platform.isCurrentPlatform(Platform.WINDOWS)) {
            return "win32"; //$NON-NLS-1$
        } else if (Platform.isCurrentPlatform(Platform.LINUX)) {
            return "linux"; //$NON-NLS-1$
        } else if (Platform.isCurrentPlatform(Platform.MAC_OS_X)) {
            return "macosx"; //$NON-NLS-1$
        } else if (Platform.isCurrentPlatform(Platform.SOLARIS)) {
            return "solaris"; //$NON-NLS-1$
        } else if (Platform.isCurrentPlatform(Platform.HPUX)) {
            return "hpux"; //$NON-NLS-1$
        } else if (Platform.isCurrentPlatform(Platform.AIX)) {
            return "aix"; //$NON-NLS-1$
        } else if (Platform.isCurrentPlatform(Platform.Z_OS)) {
            return "zos"; //$NON-NLS-1$
        } else if (Platform.isCurrentPlatform(Platform.FREEBSD)) {
            return "freebsd"; //$NON-NLS-1$
        }

        return null;
    }

    /**
     * @return the OSGI-style architecture name running this method. Returns
     *         null if the current architecture is unknown or does not map to an
     *         OSGI-style architecture name.
     */
    private static String getOSGIArchitecture() {
        final String arch = System.getProperty("os.arch"); //$NON-NLS-1$

        /*
         * JNI support for some architectures is limited to one data size
         * (32/64), so we must choose one OSGI name that covers both.
         */

        if (StringUtil.containsStringInsensitive(X86_SYNONYMS, arch)) {
            return "x86"; //$NON-NLS-1$
        } else if (StringUtil.containsStringInsensitive(AMD64_SYNONYMS, arch)) {
            return "x86_64"; //$NON-NLS-1$
        } else if (StringUtil.containsStringInsensitive(POWERPC_32_SYNONYMS, arch)
            || StringUtil.containsStringInsensitive(POWERPC_64_SYNONYMS, arch)) {
            return "ppc"; //$NON-NLS-1$
        } else if (StringUtil.containsStringInsensitive(SPARC_32_SYNONYMS, arch)
            || StringUtil.containsStringInsensitive(SPARC_64_SYNONYMS, arch)) {
            return "sparc"; //$NON-NLS-1$
        } else if (StringUtil.containsStringInsensitive(PA_RISC_20_SYNONYMS, arch)) {
            return "PA_RISC"; //$NON-NLS-1$
        } else if (StringUtil.containsStringInsensitive(ITANIUM_64_SYNONYMS, arch)) {
            return "ia64"; //$NON-NLS-1$
        } else if (StringUtil.containsStringInsensitive(ITANIUM_32_SYNONYMS, arch)) {
            return "ia64_32"; //$NON-NLS-1$
        } else if (StringUtil.containsStringInsensitive(Z_ARCH_SYNONYMS, arch)) {
            return "390"; //$NON-NLS-1$
        } else if (StringUtil.containsStringInsensitive(ARM_32_SYNONYMS, arch)) {
            return "arm"; //$NON-NLS-1$
        }

        log.trace(MessageFormat.format("Unknown value for property os.arch: {0}", arch)); //$NON-NLS-1$

        return null;
    }

    /**
     * Does a best-efford native library load with {@link #loadLibrary(String)},
     * logging whether the library loaded, and swallowing exceptions (which are
     * also logged).
     * <p>
     * Be careful how you use the return value. Many platforms will fail to load
     * the same native library twice, causing this method to return
     * <code>false</code> even though the native methods are loaded (from the
     * first load) and can be used. Worse, Java's "static" scope is
     * classloader-wide, not process wide, so it's very hard to track whether a
     * "first" call to this method succeeded and prevent any others.
     *
     * @param libraryName
     *        the library name (short name, not including extension or "lib"
     *        prefix). Not null or empty.
     * @return true if the library loaded, false if it did not
     */
    public static boolean loadLibraryAndLogError(final String libraryName) {
        Check.notNull(libraryName, "libraryName"); //$NON-NLS-1$

        try {
            NativeLoader.loadLibrary(libraryName);

            log.debug(MessageFormat.format("Successfully loaded native library {0}", libraryName)); //$NON-NLS-1$

            return true;
        } catch (final IOException e) {
            log.error(
                MessageFormat.format("IOException reading native library {0} in static initializer", libraryName), //$NON-NLS-1$
                e);
        } catch (final LinkageError e) {
            log.debug(
                MessageFormat.format("Could not load native library {0} in static initializer", libraryName), //$NON-NLS-1$
                e);
        }

        return false;
    }

    /**
     * Loads the given library name using the enhanced method documented in this
     * class's Javadoc. If a library is already loaded, subsequent calls to this
     * method with the same library name will be ignored.
     *
     * @param libraryName
     *        the library name (short name, not including extension or "lib"
     *        prefix). Not null or empty.
     * @throws UnsatisfiedLinkError
     *         if a library that maps to the given library name cannot be found.
     * @throws IOException
     *         if an error occured reading a native library resource or writing
     *         it to a temporary location.
     */
    public static void loadLibrary(final String libraryName) throws UnsatisfiedLinkError, IOException {
        Check.notNullOrEmpty(libraryName, "libraryName"); //$NON-NLS-1$

        log.debug(MessageFormat.format("Loading library {0}", libraryName)); //$NON-NLS-1$

        final String osgiOperatingSystem = getOSGIOperatingSystem();
        if (osgiOperatingSystem == null) {
            throw new UnsatisfiedLinkError(
                "Could not determine OSGI-style operating system name for resource path construction"); //$NON-NLS-1$
        }

        String osgiArchitecture = getOSGIArchitecture();
        if (osgiArchitecture == null) {
            throw new UnsatisfiedLinkError(
                "Could not determine OSGI-style architecture for resource path construction"); //$NON-NLS-1$
        }

        /*
         * Even though we make sure the OSGI architecture string we loaded is
         * non-null, we want to use a null architecture string for loading if
         * we're on Mac OS X. This is because OS X has "fat" libraries and
         * executables, and we don't want to build a path containing an
         * architecture.
         */
        if (Platform.isCurrentPlatform(Platform.MAC_OS_X)) {
            osgiArchitecture = null;
        }

        /*
         * If the system property is set, only load from that location (do not
         * fallback to other locations).
         */
        final String loadFromDirectory = System.getProperty(NATIVE_LIBRARY_BASE_DIRECTORY_PROPERTY);
        if (loadFromDirectory != null) {
            log.debug(MessageFormat.format(
                "Property {0} set to {1}; only looking there for native libraries", //$NON-NLS-1$
                NATIVE_LIBRARY_BASE_DIRECTORY_PROPERTY,
                loadFromDirectory));

            // Throws on error.
            loadLibraryFromDirectory(libraryName, loadFromDirectory, osgiOperatingSystem, osgiArchitecture);

            // Success.
            return;
        }

        /*
         * Use the Java built-in mechanisms for finding the library. This call
         * obeys java.library.path, LD_LIBRARY_PATH (Unix), DLL search path
         * (Windows), and more. It will throw if it fails.
         */
        System.loadLibrary(libraryName);
        log.info(MessageFormat.format("Loaded {0} with System.loadLibrary()", libraryName)); //$NON-NLS-1$
    }

    /**
     * Loads a native library from a directory.
     *
     * @param libraryName
     *        the library name (short name, not including extension or "lib"
     *        prefix). Not null or empty.
     * @param directory
     *        the path to the directory (not null).
     * @param osgiOperatingSystem
     *        the OSGI-style operating system name to match resources on (not
     *        null or empty).
     * @param osgiArchitecture
     *        the OSGI-style architecture name. May be null or empty to omit the
     *        architecture string from the resource path during the search. Mac
     *        OS X uses "fat" shared libraries that support multiple
     *        architectures, and would omit this parameter.
     * @throws UnsatisfiedLinkError
     *         if a library that maps to the given library name cannot be found.
     */
    private static void loadLibraryFromDirectory(
        final String libraryName,
        final String directory,
        final String osgiOperatingSystem,
        final String osgiArchitecture) {
        Check.notNullOrEmpty(libraryName, "libraryName"); //$NON-NLS-1$
        Check.notNull(directory, "directory"); //$NON-NLS-1$
        Check.notNull(osgiOperatingSystem, "osgiOperatingSystem"); //$NON-NLS-1$

        final File libraryDirectory = new File(directory);

        if (libraryDirectory.exists() == false) {
            throw new UnsatisfiedLinkError(MessageFormat.format(
                "Native library base directory {0} does not exist", //$NON-NLS-1$
                libraryDirectory.getAbsolutePath()));
        }

        if (libraryDirectory.isDirectory() == false) {
            throw new UnsatisfiedLinkError(MessageFormat.format(
                "Native library base directory {0} is not a directory", //$NON-NLS-1$
                libraryDirectory.getAbsolutePath()));
        }

        /*
         * Compute the subdirectory (i.e. "linux/ppc") inside the base
         * directory.
         */

        File fullDirectory = new File(libraryDirectory, osgiOperatingSystem);

        if (osgiArchitecture != null && osgiArchitecture.length() > 0) {
            fullDirectory = new File(fullDirectory, osgiArchitecture);
        }

        /*
         * Convert "fun" into "libfun.so" or "fun.dll" or whatever, depending on
         * platform.
         *
         * Oracle broke mapLibraryName on Java 7 so that it returns
         * libname.dylib instead of libname.jnilib. (System.loadLibrary
         * continues to use .jnilib, however, so this wasn't a change of the
         * expected extension, it's just broken.) So we need to transform names
         * ourselves on Mac OS.
         */
        final String mappedLibraryName;

        if (Platform.isCurrentPlatform(Platform.MAC_OS_X)) {
            mappedLibraryName = "lib" + libraryName + ".jnilib"; //$NON-NLS-1$ //$NON-NLS-2$
        } else {
            mappedLibraryName = System.mapLibraryName(libraryName);
        }

        /*
         * Construct the full path.
         */

        final File fullLibraryFile = new File(fullDirectory, mappedLibraryName);

        if (fullLibraryFile.exists() == false) {
            throw new UnsatisfiedLinkError(MessageFormat.format(
                "Native library file {0} does not exist", //$NON-NLS-1$
                fullLibraryFile.getAbsolutePath()));
        }

        if (fullLibraryFile.isDirectory() == true) {
            throw new UnsatisfiedLinkError(MessageFormat.format(
                "Native library file {0} is a directory", //$NON-NLS-1$
                fullLibraryFile.getAbsolutePath()));
        }

        log.debug(MessageFormat.format("Trying to load native library file {0}", fullLibraryFile.getAbsolutePath())); //$NON-NLS-1$

        System.load(fullLibraryFile.getAbsolutePath());
        log.debug(MessageFormat.format("Loaded {0} from user-specified directory", fullLibraryFile.getAbsolutePath())); //$NON-NLS-1$

        return;
    }
}
