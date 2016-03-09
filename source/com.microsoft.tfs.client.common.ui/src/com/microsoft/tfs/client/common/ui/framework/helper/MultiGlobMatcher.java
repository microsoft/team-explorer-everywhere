// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.helper;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * This class augments the GlobMatcher class to provide support for multiple
 * glob patterns. It can be used when you want to determine if a string matches
 * any one of a number of glob patterns.
 */
public class MultiGlobMatcher {
    /**
     * A convenience method to create a MultiGlobMatcher from a delimited string
     * of glob patterns. The multiPattern input is split according to the
     * specified delimiter, and the resulting pattern sections are used to
     * supply glob patterns to a MultiGlobMatcher. If the multiPattern input is
     * null or whitespace, null is returned.
     *
     * @param multiPattern
     *        a delimited string of glob patterns
     * @param delimiter
     *        the delimiter used in multiPattern
     * @return a MultiGlobMatcher made from the individual patterns or null
     */
    public static MultiGlobMatcher fromMultiPattern(final String multiPattern, final String delimiter) {
        if (multiPattern == null || multiPattern.trim().length() == 0) {
            return null;
        }

        final MultiGlobMatcher matcher = new MultiGlobMatcher();
        final String[] patterns = multiPattern.split(delimiter);
        for (int i = 0; i < patterns.length; i++) {
            matcher.addGlobPattern(patterns[i]);
        }

        return matcher;
    }

    private final List globMatchers = new ArrayList();

    /**
     * Adds a glob pattern to the glob patterns checked by this class.
     *
     * @param pattern
     *        glob pattern to add
     */
    public void addGlobPattern(final String pattern) {
        globMatchers.add(new GlobMatcher(pattern));
    }

    @Override
    public String toString() {
        return globMatchers.toString();
    }

    /**
     * @return true if the given input matches one of the glob patterns in this
     *         MultiGlobMatcher
     */
    public boolean matches(final String input) {
        for (final Iterator it = globMatchers.iterator(); it.hasNext();) {
            final GlobMatcher matcher = (GlobMatcher) it.next();
            if (matcher.matches(input)) {
                return true;
            }
        }
        return false;
    }
}
