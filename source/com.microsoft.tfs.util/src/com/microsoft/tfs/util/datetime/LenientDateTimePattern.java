// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.util.datetime;

/**
 * A pattern is a string pattern and attributes. These are later expanded by the
 * {@link LenientDateTimeParserExpander} into one or more
 * {@link LenientDateTimeFormat} instances.
 * <p>
 * This class is immutable (and therefore thread-safe).
 */
final class LenientDateTimePattern {
    private final String pattern;
    private final boolean specifiesDate;
    private final boolean specifiesTime;

    public LenientDateTimePattern(final String pattern, final boolean specifiesDate, final boolean specifiesTime) {
        this.pattern = pattern;
        this.specifiesDate = specifiesDate;
        this.specifiesTime = specifiesTime;
    }

    public boolean specifiesDate() {
        return specifiesDate;
    }

    public boolean specifiesTime() {
        return specifiesTime;
    }

    public String getPattern() {
        return pattern;
    }
}
