// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.util.datetime;

import java.text.DateFormat;
import java.util.Calendar;

import com.microsoft.tfs.util.Check;

/**
 * Holds full information about a parse match, including the Calendar that was
 * constructed and information about the format that matched. These are returned
 * by {@link LenientDateTimeParser#parseExtended(String, boolean, boolean)}.
 * <p>
 * This class is immutable (and therefore thread-safe).
 */
public class LenientDateTimeResult {
    private final Calendar calendar;
    private final DateFormat matchedDateFormat;
    private final boolean matchedDate;
    private final boolean matchedTime;

    protected LenientDateTimeResult(
        final Calendar calendar,
        final DateFormat matchedDateFormat,
        final boolean matchedDate,
        final boolean matchedTime) {
        Check.notNull(calendar, "calendar"); //$NON-NLS-1$
        Check.notNull(matchedDateFormat, "matchedDateFormat"); //$NON-NLS-1$

        this.calendar = calendar;
        this.matchedDateFormat = matchedDateFormat;
        this.matchedDate = matchedDate;
        this.matchedTime = matchedTime;
    }

    /**
     * @return the Calendar whose fields have been set according to the parsed
     *         string.
     */
    public Calendar getCalendar() {
        return calendar;
    }

    /**
     * @return true if the matching format string includes a date component (and
     *         therefore the parsed string specified a date), false if it did
     *         not.
     */
    public boolean matchedDate() {
        return matchedDate;
    }

    /**
     * @return true if the matching format string includes a time component (and
     *         therefore the parsed string specified a time), false if it did
     *         not.
     */
    public boolean matchedTime() {
        return matchedTime;
    }

    /**
     * @return the DateFormat that matched the given string.
     */
    public DateFormat getMatchedDateFormat() {
        return matchedDateFormat;
    }
}
