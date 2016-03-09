// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.util.datetime;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * Utilities for interoperability with .NET DateTime objects, including
 * serialization and deserialization in binary form.
 * <p>
 * Note that this converts with an epoch of Jan 1 1970 UTC - thus conversion
 * will always be accurate to milliseconds based on that date. The calendar date
 * may shift depending on the implementation, particularly before the Gregorian
 * reformation, due to differences in the underlying calendars. That is to say
 * that a given number of ticks may represent October 14, 1582 on .NET and
 * October 4, 1582 Java (due to that date not existing during the Gregorian
 * shift.)
 *
 */
public class DotNETDate {
    /**
     * A {@link Calendar} instance initialized to a date equivalent to the
     * DotNet DateTime.MinValue.
     */
    public static final Calendar MIN_CALENDAR;
    public static final Calendar MIN_CALENDAR_LOCAL;

    /**
     * A {@link Calendar} instance initialized to a date equivalent to the
     * DotNet DateTime.MaxValue. Calendar has less precision than DateTime
     * (milliseconds vs. 100-nanosecond ticks), so the fractional seconds value
     * of ".9999999" is represented as ".999" (999 milliseconds) in this
     * Calendar.
     */
    public static final Calendar MAX_CALENDAR;
    public static final Calendar MAX_CALENDAR_LOCAL;

    /*
     * This is the Java epoch (Jan 1 1970 00:00:00.000 UTC) in "ticks", that is
     * 100-nanosecond intervals since the Windows epoch (Jan 1 0001 00:00:00.00
     * UTC.)
     *
     * Note that this differs from the value given to MIN_CALENDAR and
     * MIN_CALENDAR_LOCAL (below.) This is due to differences in the way Java
     * and .NET handle dates before the Gregorian reformation, including a
     * difference dealing with the Gregorian shift (the days October 5, 1582
     * through October 14, 1582 did not exist in the Holy Roman Empire's
     * calendar, and also do not exist in Java's) and a difference in handling
     * leap-days before the Gregorian calendar's adoption.
     *
     * Thus, the Java epoch occurred at this number of ticks, however you cannot
     * subtract this from a Java calendar to get the .NET epoch, there's a skew
     * of two additional days. MIN_CALENDAR and MIN_CALENDAR_LOCAL handle this
     * correctly.
     */
    private static final long JAVA_EPOCH_TICKS = 621355968000000000L;

    /*
     * This is the Windows file system epoch (Jan 1 1601 00:00:00.000 UTC) in
     * "ticks", that is 100- nanosecond intervals since the .NET DateTime epoch
     * (Jan 1 0001 00:00:00.000 UTC)
     */
    private static final long WIN_FS_EPOCH_TICKS = 116444736000000000L;

    private static final long DATETIME_BINARY_FLAG_UTC = 0x4000000000000000L;
    private static final long DATETIME_BINARY_FLAG_LOCAL = 0x8000000000000000L;

    static {
        MIN_CALENDAR = Calendar.getInstance(TimeZone.getTimeZone("UTC")); //$NON-NLS-1$
        MIN_CALENDAR.setTime(new Date(-62135769600000L));

        MIN_CALENDAR_LOCAL = Calendar.getInstance();
        MIN_CALENDAR_LOCAL.setTime(new Date(-62135769600000L));

        MAX_CALENDAR = Calendar.getInstance(TimeZone.getTimeZone("UTC")); //$NON-NLS-1$
        MAX_CALENDAR.set(9999, Calendar.DECEMBER, 31, 23, 59, 59);
        MAX_CALENDAR.set(Calendar.MILLISECOND, 999);

        MAX_CALENDAR_LOCAL = Calendar.getInstance();
        MAX_CALENDAR.set(9999, Calendar.DECEMBER, 31, 23, 59, 59);
        MAX_CALENDAR.set(Calendar.MILLISECOND, 999);
    }

    private DotNETDate() {
    }

    public static long toTicks(final Calendar value) {
        /* Convert calendar's milliseconds to 100-nanosecond ticks */
        return (value.getTimeInMillis() * 10000) + JAVA_EPOCH_TICKS;
    }

    public static long toWindowsFileTimeUTC(final Calendar value) {
        return (value.getTimeInMillis() * 10000) + WIN_FS_EPOCH_TICKS;
    }

    public static Calendar fromTicks(final long ticks) {
        final Calendar calendar = Calendar.getInstance();

        calendar.setTimeInMillis((ticks - JAVA_EPOCH_TICKS) / 10000);

        return calendar;
    }

    public static Calendar fromWindowsFileTimeUTC(final long ticks) {
        final Calendar calendar = Calendar.getInstance();

        calendar.setTimeInMillis((ticks - WIN_FS_EPOCH_TICKS) / 10000);

        return calendar;
    }

    public static Calendar fromBinary(final long value) {
        Calendar calendar;

        /*
         * The .NET DateTime class can represent ticks in either UTC or "local"
         * time (though the time zone for local time is undefined), or
         * "unspecified". Fortunately, "unspecified" appears to always means UTC
         * (experimentally), so treat as such.
         */

        /* The dreaded "local" mode */
        if ((value & DATETIME_BINARY_FLAG_LOCAL) == DATETIME_BINARY_FLAG_LOCAL) {
            throw new IllegalArgumentException("Local-form DateTime serializations are not parseable"); //$NON-NLS-1$
        }

        /* UTC or unspecified mode */
        calendar = fromTicks(value & ~DATETIME_BINARY_FLAG_UTC);
        calendar.setTimeZone(TimeZone.getTimeZone("UTC")); //$NON-NLS-1$

        return calendar;
    }

    /**
     * Will create a binary serialization of the given {@link Calendar} object,
     * in UTC format, compatible with the serialization mechanisms of the .NET
     * DateTime class.
     * <p>
     * This method will never produce "local" serializations.
     *
     * @param value
     * @return
     */
    public static long toBinary(final Calendar value) {
        return toTicks(value) | DATETIME_BINARY_FLAG_UTC;
    }
}
