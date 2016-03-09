// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.util;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Utility class to determine the operating system version and perform
 * comparisons with a given version number.
 */
public final class PlatformVersion {
    private static final Log logger = LogFactory.getLog(PlatformVersion.class);
    /*
     * The cached platform version number information.
     */
    private static int[] platformVersion = null;

    /*
     * Regex to strip version number components to numeric values only.
     */
    private static Pattern componentPattern = null;

    /*
     * Integer zero to compare with
     */
    private static final Integer INTEGER_ZERO = new Integer(0);

    /**
     * Determines if the given version number is equal to the operating system
     * version number, given the rules specified by
     * {@link PlatformVersion#compareTo(String)}.
     *
     * @param versionString
     *        The version number to compare to the operating system version
     * @return true if the version numbers are equal, false otherwise
     */
    public synchronized final static boolean isCurrentVersion(final String versionString) {
        return (compareTo(versionString) == 0);
    }

    public synchronized final static boolean isLessThanVersion(final String versionString) {
        return (compareTo(versionString) < 0);
    }

    public synchronized final static boolean isLessThanOrEqualToVersion(final String versionString) {
        final int result = compareTo(versionString);

        return (result == 0 || result < 0);
    }

    public synchronized final static boolean isGreaterThanVersion(final String versionString) {
        return (compareTo(versionString) > 0);
    }

    public synchronized final static boolean isGreaterThanOrEqualToVersion(final String versionString) {
        final int result = compareTo(versionString);

        return (result == 0 || result > 0);
    }

    /**
     * Compares the given version number to the operating system version number.
     * Note that this compares the numeric portions, ignoring alpha characters
     * (and stopping at any portion of the version number that does not begin
     * with a numeric character). Thus this does not do strict string equality
     * or otherwise compare lexicographically.
     *
     * @param versionString
     *        The version number to compare with
     * @return 0 if the version numbers are equal, a value less than 0 if the
     *         operating system version number is less than the version argument
     *         and a value greater than 0 if the operating system version number
     *         is greater than the version argument
     */
    public final static int compareTo(final String versionString) {
        Check.notNull(versionString, "versionString"); //$NON-NLS-1$

        synchronized (PlatformVersion.class) {
            if (platformVersion == null) {
                platformVersion = parseVersionNumber(System.getProperty("os.version")); //$NON-NLS-1$
            }
        }

        final int[] versionCompare = parseVersionNumber(versionString);

        /*
         * Compare the individual components, starting at major version and
         * working down.
         */
        for (int i = 0; i < Math.min(platformVersion.length, versionCompare.length); i++) {
            if (platformVersion[i] < versionCompare[i]) {
                return -1;
            } else if (platformVersion[i] > versionCompare[i]) {
                return 1;
            }
        }

        /*
         * Strings are equal up to this point. If one has trailing numbers, then
         * it must be the larger version number
         */

        if (platformVersion.length < versionCompare.length) {
            return -1;
        } else if (platformVersion.length > versionCompare.length) {
            return 1;
        }

        return 0;
    }

    /**
     * Returns the integer components of the given version number string.
     * "Components" are defined as any non-alphanumeric separated string of
     * alphanumeric characters. Only the integer components are returned and
     * only up to a non-integer component. For example, given the
     * "10_4.21Q.Z.14", the return will be [ 10, 4, 21 ].
     *
     * @param versionString
     *        The version number string
     * @return An integer array of the numeric components of the version number
     *         string
     */
    static final int[] parseVersionNumber(final String versionString) {
        if (versionString == null || versionString.length() == 0) {
            return new int[0];
        }

        /*
         * Split into component pieces -- everything that is non-alphanumeric is
         * considered a version number separator.
         */
        final String[] stringComponents = versionString.split("[^a-zA-Z0-9]"); //$NON-NLS-1$
        final ArrayList versionComponents = new ArrayList();

        /*
         * We trim components to keep only the leading numeric value.
         */
        synchronized (PlatformVersion.class) {
            if (componentPattern == null) {
                try {
                    componentPattern = Pattern.compile("^([0-9]+)"); //$NON-NLS-1$
                } catch (final Exception e) {
                    logger.error("Could not compile version number regex", e); //$NON-NLS-1$
                    return new int[0];
                }
            }
        }

        for (int i = 0; i < stringComponents.length; i++) {
            /*
             * Trim to keep only the leading numeric value. If we're left with
             * nothing (ie, this component started with a letter), then do not
             * add it to the version number and stop.
             */
            final Matcher match = componentPattern.matcher(stringComponents[i]);

            if (match.find()) {
                try {
                    versionComponents.add(new Integer(stringComponents[i].substring(match.start(), match.end())));
                } catch (final Exception e) {
                    logger.warn("Could not coerce version number into format: " + versionString, e); //$NON-NLS-1$
                    break;
                }
            } else {
                /* Component did not start with a numeric value, stopping. */
                break;
            }
        }

        /* Strip trailing zeroes. */
        while (versionComponents.size() > 0
            && versionComponents.get(versionComponents.size() - 1).equals(INTEGER_ZERO)) {
            versionComponents.remove(versionComponents.size() - 1);
        }

        /* Convert to int array */
        final int[] version = new int[versionComponents.size()];

        for (int i = 0; i < versionComponents.size(); i++) {
            version[i] = ((Integer) versionComponents.get(i)).intValue();
        }

        return version;
    }
}
