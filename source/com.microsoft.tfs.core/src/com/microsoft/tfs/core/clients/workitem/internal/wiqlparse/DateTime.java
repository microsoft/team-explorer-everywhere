// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.wiqlparse;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import com.microsoft.tfs.core.clients.workitem.exceptions.WorkItemException;

public class DateTime {
    private static final String ROUNDTRIP_FORMAT_UNIVERSAL = "yyyy-MM-dd'T'HH:mm:ss.S'Z'"; //$NON-NLS-1$
    private static final String ROUNDTRIP_FORMAT_LOCAL = "yyyy-MM-dd'T'HH:mm:ss.S z"; //$NON-NLS-1$
    private static final String ROUNDTRIP_FORMAT_UNSPECIFIED = "yyyy-MM-dd'T'HH:mm:ss.S"; //$NON-NLS-1$

    public static class UncheckedParseException extends WorkItemException {
        private static final long serialVersionUID = -4518926626595967173L;

        public UncheckedParseException(final String message) {
            super(message);
        }
    }

    /**
     * Obtains a Date corresponding to the time 00:00:00.0 in the current day of
     * the specified TimeZone.
     */
    public static Date today(final TimeZone timeZone) {
        final Calendar cal = Calendar.getInstance();
        cal.setTimeZone(timeZone);

        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        return cal.getTime();
    }

    /**
     * Formats the given Date in the universal round-trip format. In this
     * format, the date is always expressed in universal time, and the string
     * always ends with a literal 'Z' character to specify this.
     */
    public static String formatRoundTripUniversal(final Date date) {
        final SimpleDateFormat sdf = new SimpleDateFormat(ROUNDTRIP_FORMAT_UNIVERSAL);
        sdf.setTimeZone(getUniversalTimeZone());
        return sdf.format(date);
    }

    /**
     * Formats the given Date in the local round-trip format. In this format,
     * the date is always expressed using the given time zone, and a time zone
     * identifier corresponding to the given time zone appears at the end of the
     * string.
     */
    public static String formatRoundTripLocal(final Date date, final TimeZone timeZone) {
        final SimpleDateFormat sdf = new SimpleDateFormat(ROUNDTRIP_FORMAT_LOCAL);
        sdf.setTimeZone(timeZone);
        return sdf.format(date);
    }

    /**
     * Formats the given Date in the unspecified round-trip format. In this
     * format, the date is always expressed using the given time zone, and no
     * time zone identifier is included in the String representation of the
     * Date.
     */
    public static String formatRoundTripUnspecified(final Date date, final TimeZone timeZone) {
        final SimpleDateFormat sdf = new SimpleDateFormat(ROUNDTRIP_FORMAT_UNSPECIFIED);
        sdf.setTimeZone(timeZone);
        return sdf.format(date);
    }

    /**
     * more-or-less equivalent to DateTime.Parse(string, CultureInfo,
     * DateTimeStyles.AssumeLocal);
     */
    public static Date parse(final String input, final Locale locale, final TimeZone timeZone) {
        final DateFormat[] formats = createDateFormatsForLocaleAndTimeZone(locale, timeZone);
        return parseWithFormats(input, formats);
    }

    public static Date parseRoundtripFormat(final String input, final TimeZone timeZone) {
        final List<DateFormat> formats = new ArrayList<DateFormat>();

        addRoundtripFormats(formats, timeZone);

        return parseWithFormats(input, formats.toArray(new DateFormat[formats.size()]));
    }

    private static Date parseWithFormats(String input, final DateFormat[] formats) {
        input = massageDateTimeString(input);

        for (int i = 0; i < formats.length; i++) {
            try {
                return formats[i].parse(input);
            } catch (final ParseException ex) {

            }
        }

        throw new UncheckedParseException(input);
    }

    /**
     * If the input string ends with a .NET time zone suffix, such as "-04:00",
     * this method will modify the input to end with a suffix that has the same
     * meaning but can be correctly parsed with SimpleDateFormat, such as "
     * GMT-04:00".
     */
    private static String massageDateTimeString(final String input) {
        if (input.length() > 6) {
            final String possibleTZSuffix = input.substring(input.length() - 6);

            final boolean dotNetLocalTZFormat = possibleTZSuffix.charAt(0) == '-'
                && Character.isDigit(possibleTZSuffix.charAt(1))
                && Character.isDigit(possibleTZSuffix.charAt(2))
                && possibleTZSuffix.charAt(3) == ':'
                && Character.isDigit(possibleTZSuffix.charAt(4))
                && Character.isDigit(possibleTZSuffix.charAt(5));

            if (dotNetLocalTZFormat) {
                return input.substring(0, input.length() - 6) + " GMT" + possibleTZSuffix; //$NON-NLS-1$
            }
        }
        return input;
    }

    private static DateFormat[] createDateFormatsForLocaleAndTimeZone(Locale locale, TimeZone timeZone) {
        if (locale == null) {
            locale = Locale.getDefault();
        }

        if (timeZone == null) {
            timeZone = TimeZone.getDefault();
        }

        final List<DateFormat> formats = new ArrayList<DateFormat>();

        addRoundtripFormats(formats, timeZone);

        for (int dateStyle = DateFormat.FULL; dateStyle <= DateFormat.SHORT; dateStyle++) {
            for (int timeStyle = DateFormat.FULL; timeStyle <= DateFormat.SHORT; timeStyle++) {
                final DateFormat df = DateFormat.getDateTimeInstance(dateStyle, timeStyle, locale);
                if (timeZone != null) {
                    df.setTimeZone(timeZone);
                }
                formats.add(df);
            }
        }

        for (int dateStyle = DateFormat.FULL; dateStyle <= DateFormat.SHORT; dateStyle++) {
            final DateFormat df = DateFormat.getDateInstance(dateStyle, locale);
            df.setTimeZone(timeZone);
            formats.add(df);
        }

        /*
         * System.out.println("formats for locale [" + locale + "] timezone [" +
         * timeZone.getID() + "]"); for (Iterator it = formats.iterator();
         * it.hasNext(); ) { DateFormat df = (DateFormat) it.next(); if (df
         * instanceof SimpleDateFormat) { System.out.println(((SimpleDateFormat)
         * df).toPattern()); } else { System.out.println("unknown (" +
         * df.getClass().getName() + ")"); } }
         */

        return formats.toArray(new DateFormat[formats.size()]);
    }

    private static void addRoundtripFormats(final List<DateFormat> formats, final TimeZone timeZone) {
        SimpleDateFormat sdf = new SimpleDateFormat(ROUNDTRIP_FORMAT_UNIVERSAL);
        sdf.setTimeZone(getUniversalTimeZone());
        formats.add(sdf);

        sdf = new SimpleDateFormat(ROUNDTRIP_FORMAT_LOCAL);
        /*
         * don't need to .setTimeZone on this SDF since TZ is in its format
         * specifier and this SDF is used for PARSING only
         */
        formats.add(sdf);

        sdf = new SimpleDateFormat(ROUNDTRIP_FORMAT_UNSPECIFIED);
        sdf.setTimeZone(timeZone);
        formats.add(sdf);
    }

    private static TimeZone getUniversalTimeZone() {
        return TimeZone.getTimeZone("UTC"); //$NON-NLS-1$
    }
}
