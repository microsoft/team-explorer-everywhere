// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teambuild.teamexplorer.helpers;

import java.text.MessageFormat;
import java.util.Calendar;

import com.microsoft.tfs.client.common.ui.teambuild.Messages;
import com.microsoft.tfs.util.Check;

public class TimeSpanHelpers {
    private static final double MINUTE_IN_SECONDS = 60;
    private static final double HOUR_IN_SECONDS = MINUTE_IN_SECONDS * 60;
    private static final double DAY_IN_SECONDS = HOUR_IN_SECONDS * 24;
    private static final double WEEK_IN_SECONDS = DAY_IN_SECONDS * 7;
    private static final double MONTH_IN_SECONDS = (DAY_IN_SECONDS * 365) / 12;
    private static final double YEAR_IN_SECONDS = DAY_IN_SECONDS * 365;

    private static class AgoFormatSpec {
        public double limit;
        public String format;
        public double divisibleArgument;

        public AgoFormatSpec(final double limit, final String format, final double divisibleArgument) {
            this.limit = limit;
            this.format = format;
            this.divisibleArgument = divisibleArgument;
        }
    }

    private static final AgoFormatSpec[] AGO_FORMAT_SPECS = new AgoFormatSpec[] {
        new AgoFormatSpec(MINUTE_IN_SECONDS, Messages.getString("TimeSpanHelpers.DateTimeAgoSecondsFormat"), 1), //$NON-NLS-1$
        new AgoFormatSpec(MINUTE_IN_SECONDS * 1.5, Messages.getString("TimeSpanHelpers.DateTimeAgoAMinute"), 0), //$NON-NLS-1$
        new AgoFormatSpec(
            HOUR_IN_SECONDS,
            Messages.getString("TimeSpanHelpers.DateTimeAgoMinutesFormat"), //$NON-NLS-1$
            MINUTE_IN_SECONDS),
        new AgoFormatSpec(HOUR_IN_SECONDS * 1.5, Messages.getString("TimeSpanHelpers.DateTimeAgoAnHour"), 0), //$NON-NLS-1$
        new AgoFormatSpec(
            DAY_IN_SECONDS,
            Messages.getString("TimeSpanHelpers.DateTimeAgoHoursFormat"), //$NON-NLS-1$
            HOUR_IN_SECONDS),
        new AgoFormatSpec(DAY_IN_SECONDS * 1.5, Messages.getString("TimeSpanHelpers.DateTimeAgoADay"), 0), //$NON-NLS-1$
        new AgoFormatSpec(WEEK_IN_SECONDS, Messages.getString("TimeSpanHelpers.DateTimeAgoDaysFormat"), DAY_IN_SECONDS), //$NON-NLS-1$
        new AgoFormatSpec(WEEK_IN_SECONDS * 1.5, Messages.getString("TimeSpanHelpers.DateTimeAgoAWeek"), 0), //$NON-NLS-1$
        new AgoFormatSpec(
            MONTH_IN_SECONDS,
            Messages.getString("TimeSpanHelpers.DateTimeAgoWeeksFormat"), //$NON-NLS-1$
            WEEK_IN_SECONDS),
        new AgoFormatSpec(MONTH_IN_SECONDS * 1.5, Messages.getString("TimeSpanHelpers.DateTimeAgoAMonth"), 0), //$NON-NLS-1$
        new AgoFormatSpec(
            YEAR_IN_SECONDS,
            Messages.getString("TimeSpanHelpers.DateTimeAgoMonthsFormat"), //$NON-NLS-1$
            MONTH_IN_SECONDS),
        new AgoFormatSpec(YEAR_IN_SECONDS * 1.5, Messages.getString("TimeSpanHelpers.DateTimeAgoAYear"), 0), //$NON-NLS-1$
        new AgoFormatSpec(
            Double.MAX_VALUE,
            Messages.getString("TimeSpanHelpers.DateTimeAgoYearsFormat"), //$NON-NLS-1$
            YEAR_IN_SECONDS),
    };

    /**
     * Returns a string to describe the time interval between this date and the
     * present DateTime.
     */
    public static String ago(final Calendar date) {
        final Calendar now = Calendar.getInstance();
        final double seconds = (now.getTimeInMillis() - date.getTimeInMillis()) / 1000;
        return formatTimeSpan(seconds);
    }

    public static String duration(final Calendar start, final Calendar end) {
        final double seconds = (end.getTimeInMillis() - start.getTimeInMillis()) / 1000;
        Check.isTrue(seconds >= 0, "start date is newer than end date"); //$NON-NLS-1$

        if (seconds < MINUTE_IN_SECONDS) {
            final String format = Messages.getString("TimeSpanHelpers.DurationSecondsFormat"); //$NON-NLS-1$
            return MessageFormat.format(format, MessageFormat.format("{0,number,#.#}", seconds)); //$NON-NLS-1$
            // seconds
        } else if (seconds < HOUR_IN_SECONDS) {
            final double minutes = seconds / MINUTE_IN_SECONDS;
            final String format = Messages.getString("TimeSpanHelpers.DurationMinutesFormat"); //$NON-NLS-1$
            return MessageFormat.format(format, MessageFormat.format("{0,number,#.#}", minutes)); //$NON-NLS-1$
        } else {
            final double hours = seconds / HOUR_IN_SECONDS;
            final String format = Messages.getString("TimeSpanHelpers.DurationHoursFormat"); //$NON-NLS-1$
            return MessageFormat.format(format, MessageFormat.format("{0,number,#.#}", hours)); //$NON-NLS-1$
        }
    }

    /**
     * Returns the first time interval string match within the format spec limit
     * as "... ago".
     */
    private static String formatTimeSpan(final double seconds) {
        for (final AgoFormatSpec step : AGO_FORMAT_SPECS) {
            double timeDelta = seconds;
            double limit = step.limit;

            // If there is a divisible argument, divide both the time and the
            // limit to avoid rounding issues when doing the comparison
            // (otherwise you can get strings like '60 minutes ago' instead of
            // 'one hour ago' due to rounding issues)
            if (step.divisibleArgument > 0) {
                timeDelta = Math.round(timeDelta / step.divisibleArgument);
                limit = Math.round(limit / step.divisibleArgument);
            }

            if (timeDelta < limit) {
                if (step.divisibleArgument > 0) {
                    return MessageFormat.format(step.format, timeDelta);
                } else {
                    return step.format;
                }
            }
        }

        throw new IllegalArgumentException("seconds"); //$NON-NLS-1$
    }
}
