// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.util;

/**
 * Contains static methods to classify the current platform.
 *
 * @threadsafety thread-safe
 */
public final class Platform extends BitField {
    /**
     * The cached platform string.
     */
    private static String platformString = null;

    /**
     * Caches the matching platforms to make subsequent calls faster.
     */
    private static Platform matchingPlatforms = null;

    /**
     * Gets whether the current platform "is" the given platform. Multiple
     * platforms may evaluate to true. For instance this method returns true
     * when running on a JVM on Linux if given either
     * {@link Platform#GENERIC_UNIX} or {@link Platform#LINUX}.
     *
     * @param platform
     *        the platform to test for the existence of (not null). Multiple
     *        platforms may match on a given JVM.
     * @return the type of platform where this program is running.
     */
    public synchronized final static boolean isCurrentPlatform(final Platform platform) {
        Check.notNull(platform, "platform"); //$NON-NLS-1$

        return getCurrentPlatform().contains(platform);
    }

    /**
     * Gets a {@link Platform} which describes the current running environment,
     * which may be a combination of multiple public final fields in this class.
     * This method exists mainly to aid in testing. It's better to call
     * {@link #isCurrentPlatform(Platform)} in most code to test whether a
     * platform is present.
     *
     * @return the {@link Platform} which describes the current running
     *         environment, which may be a combination of one or more public
     *         final fields in this class (never <code>null</code>)
     */
    public synchronized final static Platform getCurrentPlatform() {
        if (matchingPlatforms == null) {
            final String os = System.getProperty("os.name"); //$NON-NLS-1$

            if (os.startsWith("Windows") == true) //$NON-NLS-1$
            {
                matchingPlatforms = WINDOWS;

                // TODO Do we want to detect and define specific Windows
                // releases?
            } else {
                matchingPlatforms = GENERIC_UNIX;

                if (os.startsWith("Mac OS X")) //$NON-NLS-1$
                {
                    matchingPlatforms = matchingPlatforms.combine(MAC_OS_X);
                }

                if (os.startsWith("Linux")) //$NON-NLS-1$
                {
                    matchingPlatforms = matchingPlatforms.combine(LINUX);
                }

                if (os.startsWith("SunOS")) //$NON-NLS-1$
                {
                    matchingPlatforms = matchingPlatforms.combine(SOLARIS);
                }

                if (os.startsWith("AIX")) //$NON-NLS-1$
                {
                    matchingPlatforms = matchingPlatforms.combine(AIX);
                }

                if (os.startsWith("HP-UX")) //$NON-NLS-1$
                {
                    matchingPlatforms = matchingPlatforms.combine(HPUX);
                }

                if (os.startsWith("z/OS")) //$NON-NLS-1$
                {
                    matchingPlatforms = matchingPlatforms.combine(Z_OS);
                }

                if (os.startsWith("FreeBSD")) //$NON-NLS-1$
                {
                    matchingPlatforms = matchingPlatforms.combine(FREEBSD);
                }
            }
        }

        return matchingPlatforms;
    }

    /**
     * Gets the current platform string. Do not match against this string; it's
     * mostly here for when Platform does not define <b>any</b> matching
     * platforms and we must report an error so the user can report it to us.
     *
     * @return a string which you should not match against that describes the
     *         current plaform.
     */
    public synchronized final static String getCurrentPlatformString() {
        if (platformString == null) {
            platformString = System.getProperty("os.name"); //$NON-NLS-1$
        }

        return platformString;
    }

    public static Platform combine(final Platform[] changeTypes) {
        return new Platform(BitField.combine(changeTypes));
    }

    public final static Platform NONE = new Platform(0, "NONE"); //$NON-NLS-1$

    /**
     * Any Microsoft Windows platform will match.
     */
    public final static Platform WINDOWS = new Platform(1, "WINDOWS"); //$NON-NLS-1$

    /**
     * Any generic Unix platform will match.
     */
    public final static Platform GENERIC_UNIX = new Platform(2, "GENERIC_UNIX"); //$NON-NLS-1$

    /**
     * Apple Mac OS X.
     */
    public final static Platform MAC_OS_X = new Platform(4, "MAC_OS_X"); //$NON-NLS-1$

    /**
     * Linux.
     */
    public final static Platform LINUX = new Platform(8, "LINUX"); //$NON-NLS-1$

    /**
     * Sun Solaris.
     */
    public final static Platform SOLARIS = new Platform(16, "SOLARIS"); //$NON-NLS-1$

    /**
     * IBM AIX.
     */
    public final static Platform AIX = new Platform(32, "AIX"); //$NON-NLS-1$

    /**
     * HP-UX.
     */
    public final static Platform HPUX = new Platform(64, "HPUX"); //$NON-NLS-1$

    /**
     * IBM z/OS which is just "Unix enough" but uses EBCDIC character encoding.
     */
    public final static Platform Z_OS = new Platform(128, "Z_OS"); //$NON-NLS-1$

    /**
     * FreeBSD
     */
    public final static Platform FREEBSD = new Platform(256, "FREEBSD"); //$NON-NLS-1$

    private Platform(final int flags, final String name) {
        super(flags);
        registerStringValue(getClass(), flags, name);
    }

    private Platform(final int flags) {
        super(flags);
    }

    public boolean containsAll(final Platform other) {
        return containsAllInternal(other);
    }

    public boolean contains(final Platform other) {
        return containsInternal(other);
    }

    public boolean containsAny(final Platform other) {
        return containsAnyInternal(other);
    }

    public Platform remove(final Platform other) {
        return new Platform(removeInternal(other));
    }

    public Platform retain(final Platform other) {
        return new Platform(retainInternal(other));
    }

    public Platform combine(final Platform other) {
        return new Platform(combineInternal(other));
    }
}
