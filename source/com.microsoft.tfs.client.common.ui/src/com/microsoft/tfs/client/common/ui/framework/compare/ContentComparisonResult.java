// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.compare;

/**
 * {@link ContentComparisonResult} is returned by a {@link ContentComparator} as
 * the result of a content comparison operation between two differencer input
 * objects.
 */
public class ContentComparisonResult {
    /**
     * A result that indicates that the two objects have equal content.
     */
    public static final ContentComparisonResult EQUAL = new ContentComparisonResult("EQUAL"); //$NON-NLS-1$

    /**
     * A result that indicates that the two objects have unequal content.
     */
    public static final ContentComparisonResult NOT_EQUAL = new ContentComparisonResult("NOT_EQUAL"); //$NON-NLS-1$

    /**
     * A result that indicates that the content could not be compared by the
     * comparator that returned this result.
     */
    public static final ContentComparisonResult UNKNOWN = new ContentComparisonResult("UNKNOWN"); //$NON-NLS-1$

    private final String s;

    private ContentComparisonResult(final String s) {
        this.s = s;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return s;
    }
}
