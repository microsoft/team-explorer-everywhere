// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.util.datetime;

import java.util.Calendar;

/**
 * Some useful functions when dealing with {@link Calendar}s.
 */
public class CalendarUtils {

    /**
     * Remote the time elements of the passed {@link Calendar}, setting the time
     * to midnight of that day. Note: This function will affect the date passed
     * as well as returning the same date. If you do not wish this method to
     * affect the date passed then you should clone it before passing.
     *
     * @param date
     *        The date that you wish to remove the time portions from.
     * @return the same {@link Calendar} istance passed, with the time fields
     *         now set to 0.
     */
    public static Calendar removeTime(final Calendar date) {
        date.set(Calendar.SECOND, 0);
        date.set(Calendar.MINUTE, 0);
        date.set(Calendar.HOUR, 0);
        date.set(Calendar.HOUR_OF_DAY, 0);
        date.set(Calendar.MILLISECOND, 0);

        // Note that this line is needed due to a strange issue in Gregorian
        // Calendar.
        // If this is not done then subsequent additions to the time etc can be
        // ignored.
        date.getTimeInMillis();

        return date;
    }

    public static boolean equalsIgnoreTimeZone(final Calendar c1, final Calendar c2) {
        if (c1 == null && c2 == null) {
            return true;
        } else if (c1 == null || c2 == null) {
            return false;
        }

        /*
         * Don't retrieve fields like .get(Calendar.MONTH) as that adjusts for
         * time zone. Internal milliseconds don't account for TZ.
         */
        return c1.getTimeInMillis() == c2.getTimeInMillis();
    }

    /**
     * @return the number of seconds since midnight, as calculated by the
     *         {@link Calendar#HOUR_OF_DAY}, {@link Calendar#MINUTE} and
     *         {@link Calendar#SECOND} fields in the passed {@link Calendar}.
     */
    public static int getSecondsSinceMidnight(final Calendar time) {
        return time.get(Calendar.SECOND) + (time.get(Calendar.MINUTE) * 60) + (time.get(Calendar.HOUR_OF_DAY) * 3600);
    }

}
