// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.path;

/**
 * Static methods to match strings against TFS path wildcard characters.
 *
 * @threadsafety thread-safe
 * @since TEE-SDK-10.1
 */
public final class Wildcard {
    /**
     * Supported wildcard characters.
     */
    private final static char[] WILDCARD_CHARS = new char[] {
        '*',
        '?'
    };

    /**
     * Tests whether the given string contains wildcards. This method does not
     * consider whether the given string is a server or local path, and will
     * return true if any wildcard character is found inside it. For paths,
     * usually only the last component should be considered when testing for
     * wildcards (because of TFS wildcard rules). Use
     * {@link ServerPath#isWildcard(String)} or
     * {@link LocalPath#isWildcard(String)} methods instead of this one for that
     * purpose.
     *
     * @param string
     *        the string to test (if string is null, return value is false)
     * @return true if the string contains wildcard characters, false if it does
     *         not or the given string was null
     */
    public static boolean isWildcard(final String string) {
        if (string == null) {
            return false;
        }

        for (int i = 0; i < Wildcard.WILDCARD_CHARS.length; ++i) {
            if (string.indexOf(Wildcard.WILDCARD_CHARS[i]) != -1) {
                return true;
            }
        }

        return false;
    }

    /**
     * Tests whether the given character is a wildcard character
     *
     * @param ch
     *        the character to test
     * @return true if the given character is a wildcard, false if it is not
     */
    public static boolean isWildcard(final char ch) {
        for (int i = 0; i < Wildcard.WILDCARD_CHARS.length; ++i) {
            if (ch == Wildcard.WILDCARD_CHARS[i]) {
                return true;
            }
        }
        return false;
    }
}
