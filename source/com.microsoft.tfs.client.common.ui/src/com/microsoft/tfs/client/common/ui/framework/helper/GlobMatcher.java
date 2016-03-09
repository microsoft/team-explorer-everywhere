// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.helper;

import java.util.regex.Pattern;

/**
 * A very simple glob pattern matcher. This class is useful for matching file
 * names against glob patterns like "*.txt".
 *
 * Right now, the glob patterns supported are very simple. Only two
 * metacharacters are supported: ? - means any one character * - means 0 or more
 * of any characters
 *
 * More complex glob expression support could be added in the future if we need
 * it. Another alternative to this class would be using the Jakarta ORO library,
 * which contains a rich glob pattern matcher.
 */
public class GlobMatcher {
    private final String globPattern;
    private final String regex;
    private final Pattern pattern;

    /**
     * Creates a new GlobMatcher using the given glob pattern.
     *
     * @param globPattern
     *        pattern to use for this matcher
     */
    public GlobMatcher(final String globPattern) {
        this.globPattern = globPattern;
        regex = createRegex(globPattern);
        pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
    }

    @Override
    public String toString() {
        return globPattern;
    }

    /**
     * @return true if the given input matches this GlobMatcher's glob pattern
     */
    public boolean matches(final String input) {
        return pattern.matcher(input).matches();
    }

    private String createRegex(final String pattern) {
        /*
         * This algorithm creates a java regex string based off a glob pattern
         * string. The glob pattern supported can contain any combination of two
         * metacharacters: ? - any one character * - 0 or more characters This
         * glob pattern is transformed into a java regex string by the following
         * rules: 1) The '?' glob metacharacters is equivalent to the '.' java
         * regex metacharacter 2) The '*' glob metacharacters is equivalent to
         * the ".*" java regex expression 3) All other portions of the glob
         * pattern become java regex quoted literals (\Q\E)
         */
        final StringBuffer regex = new StringBuffer();
        StringBuffer currentLiteral = new StringBuffer();

        for (int i = 0; i < pattern.length(); i++) {
            final char c = pattern.charAt(i);
            if (c == '?' || c == '*') // glob metacharacters
            {
                if (currentLiteral.length() > 0) {
                    regex.append("\\Q").append(currentLiteral).append("\\E"); //$NON-NLS-1$ //$NON-NLS-2$
                    currentLiteral = new StringBuffer();
                }
                if (c == '?') {
                    regex.append("."); //$NON-NLS-1$
                } else {
                    regex.append(".*"); //$NON-NLS-1$
                }
            } else {
                currentLiteral.append(c);
            }
        }
        if (currentLiteral.length() > 0) {
            regex.append("\\Q").append(currentLiteral).append("\\E"); //$NON-NLS-1$ //$NON-NLS-2$
        }

        return regex.toString();
    }
}
