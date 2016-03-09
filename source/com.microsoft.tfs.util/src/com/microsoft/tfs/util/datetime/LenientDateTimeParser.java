// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.util.datetime;

import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.Messages;

/**
 * Attemps to parse a string containing a date and/or time into a Calendar
 * according to all the date format definitions it knows. Flags may be passed to
 * the parse method to use a default time if the given string contains a date
 * but not time, or use a default date if it contains a time but not date. An
 * exception is thrown if the parse did not match any available formats.
 * <p>
 * All parsing is done internally using SimpleDateFormat many instances arranged
 * in a list. The order in which they are matched against is:
 * <p>
 * <ol>
 * <li>SimpleDateFormat.getDateTimeInstance() for many precisions for the
 * instance's locale is tried</li>
 * <li>ISO8601 and ISO8601-like formats are tried next ("year-month-day")</li>
 * <li>If the instance's locale prefers "month-day-year" formats (United
 * States), those are tried next; if it prefers "day-month-year" instead, those
 * are tried next. Then the other (European or US) is tried.</li>
 * <li>Generic date-only formats (via SimpleDateFormat.getDateInstance()) for
 * the instance's locale are tried</li>
 * <li>Generic time-only formats (via SimpleDateFormat.getTimeInstance()) for
 * the instance's locale are tried</li>
 * </ol>
 * <p>
 * In total, many hundreds of formats are matched against, comprising all
 * commonly used combinations of precision, component ordering, different date
 * and time separators, month names vs. numbers, time zone presence, etc.
 * Because of the large number of formats constructed and matched, creating an
 * instance of LenientDateTimeParser may be a computationally intensive
 * operation. Consider reusing instances you create instead of creating many new
 * ones.
 * <p>
 * <b>"Lenient"</b>
 * <p>
 * The name "Lenient" as it applies to this class means "accepting of many
 * different date and time formats." It does not mean the same as the lenient
 * property of DateFormat, which means something like "nonsensical dates like
 * 2007/01/45 really mean 14 February 2007." The LenientDateTimeParser class
 * uses non-lenient SimpleDateFormat instances to do its parsing. Dates like
 * "2007/01/45" will not parse (unless some locale I don't know about accepts
 * it).
 * <p>
 * <b>Notes on Time Zones</b>
 * <p>
 * Each LenientDateTimeParser instance has an associated time zone (if not
 * specified during construction, it is the default time zone). When a string
 * <b>without</b> time zone is parsed, the returned Calendar has its time zone
 * initialized to the {@link LenientDateTimeParser} instance's associated time
 * zone. When a string <b>with</b> a time zone is parsed, the
 * {@link LenientDateTimeParser} instance's associated time zone is ignored and
 * the returned Calendar is in the time zone present in the input string.
 * <p>
 * This class is immutable (and therefore thread-safe).
 */
public class LenientDateTimeParser {
    private static final Log log = LogFactory.getLog(LenientDateTimeParser.class);

    /**
     * These formats are internationally recognized date formats. They will be
     * expanded in all the ways described by LenientDateTimeParserExpander.
     *
     * As a general rule, put more precise formats first in these arrays. Less
     * precise formats will match prematurely and cause additional precision in
     * the candidate string to be ignored.
     *
     * 12-hour time formats must include AM/PM marker.
     *
     * Milliseconds are generally not included (because TFS doesn't match with
     * that precision) except where standards require them (ISO8601).
     */

    // ISO8601 and variants
    private static final LenientDateTimePattern[] isoDateTimeFormats = new LenientDateTimePattern[]

    {
        // This comment blank to make the formatter happy.
        new LenientDateTimePattern("yyyy/MM/dd'T'HH:mm:ss.SSSz", true, true), // ISO8601-Standard //$NON-NLS-1$
        new LenientDateTimePattern("yyyy/MM/dd'T'HH:mm:ssz", true, true), // ISO8601-like //$NON-NLS-1$
        new LenientDateTimePattern("yyyy/MM/dd'T'HH:mm:ss", true, true), // ISO8601-like //$NON-NLS-1$
        new LenientDateTimePattern("yyyy/MM/dd'T'HH:mmz", true, true), // ISO8601-like //$NON-NLS-1$
        new LenientDateTimePattern("yyyy/MM/dd'T'HH:mm", true, true), // ISO8601-like //$NON-NLS-1$
    };

    // US date style
    private static final LenientDateTimePattern[] usDateFormats = new LenientDateTimePattern[]

    {
        // This comment blank to make the formatter happy.
        new LenientDateTimePattern("MM/dd/yy h:mm:ss a z", true, true), //$NON-NLS-1$
        new LenientDateTimePattern("MM/dd/yy h:mm:ss a", true, true), //$NON-NLS-1$
        new LenientDateTimePattern("MM/dd/yy h:mm a z", true, true), //$NON-NLS-1$
        new LenientDateTimePattern("MM/dd/yy h:mm a", true, true), //$NON-NLS-1$

        new LenientDateTimePattern("MM/dd/yy HH:mm:ss z", true, true), //$NON-NLS-1$
        new LenientDateTimePattern("MM/dd/yy HH:mm:ss", true, true), //$NON-NLS-1$
        new LenientDateTimePattern("MM/dd/yy HH:mm z", true, true), //$NON-NLS-1$
        new LenientDateTimePattern("MM/dd/yy HH:mm", true, true), //$NON-NLS-1$

        new LenientDateTimePattern("MM/dd/yy", true, false) //$NON-NLS-1$

    };

    // European date style
    private static final LenientDateTimePattern[] euDateFormats = new LenientDateTimePattern[]

    {
        // This comment blank to make the formatter happy.
        new LenientDateTimePattern("d/MM/yy h:mm:ss a z", true, true), //$NON-NLS-1$
        new LenientDateTimePattern("d/MM/yy h:mm:ss a", true, true), //$NON-NLS-1$
        new LenientDateTimePattern("d/MM/yy h:mm a z", true, true), //$NON-NLS-1$
        new LenientDateTimePattern("d/MM/yy h:mm a", true, true), //$NON-NLS-1$

        new LenientDateTimePattern("d/MM/yy HH:mm:ss z", true, true), //$NON-NLS-1$
        new LenientDateTimePattern("d/MM/yy HH:mm:ss", true, true), //$NON-NLS-1$
        new LenientDateTimePattern("d/MM/yy HH:mm z", true, true), //$NON-NLS-1$
        new LenientDateTimePattern("d/MM/yy HH:mm", true, true), //$NON-NLS-1$

        new LenientDateTimePattern("d/MM/yy", true, false) //$NON-NLS-1$

    };

    /*
     * Times only. These should be matched last because they're rarer, and they
     * may cause a false positive parse match with what should be a date
     * (because '.' is sometimes used as a separator for dates and times and
     * SimpleDateFormat is eager to match long inputs with short patterns).
     */
    private static final LenientDateTimePattern[] genericTimeFormats = new LenientDateTimePattern[]

    {
        // This comment blank to make the formatter happy.
        new LenientDateTimePattern("h:mm:ss a z", false, true), //$NON-NLS-1$
        new LenientDateTimePattern("h:mm:ss a", false, true), //$NON-NLS-1$
        new LenientDateTimePattern("h:mm a z", false, true), //$NON-NLS-1$
        new LenientDateTimePattern("h:mm a", false, true), //$NON-NLS-1$

        new LenientDateTimePattern("HH:mm:ss z", false, true), //$NON-NLS-1$
        new LenientDateTimePattern("HH:mm:ssz", false, true), //$NON-NLS-1$
        new LenientDateTimePattern("HH:mm:ss", false, true), //$NON-NLS-1$
        new LenientDateTimePattern("HH:mm z", false, true), //$NON-NLS-1$
        new LenientDateTimePattern("HH:mmz", false, true), //$NON-NLS-1$
        new LenientDateTimePattern("HH:mm", false, true) //$NON-NLS-1$

    };

    /**
     * This array holds the sorted results of the static expansion code defined
     * in this class. The contents are all the format strings we'll test against
     * when parsing a date.
     */
    private LenientDateTimeFormat[] expandedFormats = new LenientDateTimeFormat[0];

    private final TimeZone timeZone;
    private final Locale locale;

    /**
     * These time zone names are equivalent to UTC but must be converted to a
     * string like "UTC" because {@link SimpleDateFormat} can't parse them
     * normally.
     */
    private final String[] additionalUTCTimeZoneStrings = new String[] {
        "Z", //$NON-NLS-1$
        "z" //$NON-NLS-1$
    };

    /**
     * Constructs a lenient parser for the default locale and time zone.
     */
    public LenientDateTimeParser() {
        timeZone = TimeZone.getDefault();
        locale = Locale.getDefault();

        computeFormats();
    }

    /**
     * Constructs a lenient parser for the given time zone and locale.
     *
     * @param timeZone
     *        the time zone where the returned dates and times are rooted (not
     *        null).
     * @param locale
     *        the locale to use for parsing and matching patterns as well as
     *        generated calendars (not null).
     */
    public LenientDateTimeParser(final TimeZone timeZone, final Locale locale) {
        Check.notNull(timeZone, "timeZone"); //$NON-NLS-1$
        Check.notNull(locale, "locale"); //$NON-NLS-1$

        this.timeZone = timeZone;
        this.locale = locale;

        computeFormats();
    }

    /**
     * Parse a free-form date and time first with the default format for the
     * default locale, then by parsing a list of known formats until one
     * succeeds. If the date cannot be parsed, ParseException is thrown.
     * <p>
     * WARNING: GMT doesn't parse as a time zone in on my platform (JDK 1.5,
     * Linux). Use UTC in your input instead.
     * <p>
     * This method may be somewhat slow, although it is probably not an area of
     * concern for most programs. Because of the way SimpleDateFormat parses,
     * all of the longer, more precise patterns must be tried first (and these
     * are least likely to be used by the user). This means we must walk an
     * array for each search and common formats are necessarily near the end. In
     * reality, most searches can be done under a few (6) milliseconds on a 3
     * GHz computer.
     * <p>
     * <b>Notes on Default Date and Time</b>
     * <p>
     * The defaultDate and defaultTime parameters control whether the returned
     * Calendar has its date and/or time components set to the current date
     * and/or time (instead of the Date class epoch) when the given date time
     * string matches a pattern that does not specify <b>both</b> date and time
     * components. These parameters do <b>not</b> affect the order in which
     * patterns are tested or the criteria for a pattern match. They simply
     * choose which date or time is returned in the date- or time-only pattern
     * match where one of the components was unspecified. When either
     * defaultDate or defaultTime is false the Date class's epoch (1970/01/01
     * 00:00:00 UTC) is used for the default date or time.
     * <p>
     * Both defaultDate and defaultTime may be set to true, but as these
     * parameters do not affect the matching behavior, a given string missing
     * <b>both</b> date and time components will still match no patterns
     * (causing a ParseException to be thrown).
     *
     * @param dateTimeString
     *        the date string to parse (not null).
     * @param defaultDate
     *        if true and the given date time string matches a time-only
     *        pattern, today's date is used in the returned Calendar. If false
     *        and a time-only pattern is matched, 1970/01/01 is used for the
     *        date. See the method Javadoc for more information.
     * @param defaultTime
     *        if true and the given date time string matches a date-only
     *        pattern, the current time is used in the returned Calendar. If
     *        false and a date-only pattern is matched, 00:00:00 is used for the
     *        date. See the method Javadoc for more information.
     * @return a {@link LenientDateTimeResult} containing the date/time that was
     *         in the given dateTimeString and additional match information.
     * @throws ParseException
     *         if the date time string could not be parsed.
     */
    public LenientDateTimeResult parseExtended(
        String dateTimeString,
        final boolean defaultDate,
        final boolean defaultTime) throws ParseException {
        Check.notNull(dateTimeString, "dateTimeString"); //$NON-NLS-1$

        final long start = System.currentTimeMillis();
        int i = 0;
        Calendar calendar = Calendar.getInstance(timeZone, locale);
        calendar.clear();

        // Java's SimpleDateFormat.parse() doesn't support some common time
        // zones, so we convert them before parsing.
        dateTimeString = convertUnsupportedTimeZones(dateTimeString);

        for (i = 0; i < expandedFormats.length; i++) {
            final LenientDateTimeFormat format = expandedFormats[i];

            try {
                calendar.setTime(format.getDateFormat().parse(dateTimeString));

                if (defaultDate && format.specifiesDate() == false) {
                    /*
                     * We matched a pattern that doesn't specify a date and the
                     * user wants a default of today. Copy now's date info into
                     * ret.
                     */

                    final Calendar now = new GregorianCalendar(timeZone, locale);
                    calendar.set(Calendar.ERA, now.get(Calendar.ERA));
                    calendar.set(Calendar.YEAR, now.get(Calendar.YEAR));
                    calendar.set(Calendar.MONTH, now.get(Calendar.MONTH));
                    calendar.set(Calendar.DATE, now.get(Calendar.DATE));
                }

                if (defaultTime && format.specifiesTime() == false) {
                    /*
                     * We matched a pattern that doesn't specify a time and the
                     * user wants a default of now. Copy the parsed Calendar's
                     * date info into a new time.
                     */

                    final Calendar newRet = new GregorianCalendar(timeZone, locale);
                    newRet.set(Calendar.ERA, calendar.get(Calendar.ERA));
                    newRet.set(Calendar.YEAR, calendar.get(Calendar.YEAR));
                    newRet.set(Calendar.MONTH, calendar.get(Calendar.MONTH));
                    newRet.set(Calendar.DATE, calendar.get(Calendar.DATE));

                    calendar = newRet;
                }

                final String messageFormat = "matched at index {0} in {1} ms: {2}"; //$NON-NLS-1$
                final String message =
                    MessageFormat.format(messageFormat, i, (System.currentTimeMillis() - start), dateTimeString);
                log.trace(message);

                return new LenientDateTimeResult(
                    calendar,
                    format.getDateFormat(),
                    format.specifiesDate(),
                    format.specifiesTime());
            } catch (final ParseException e) {
                // Ignore and keep looping.
            }
        }

        String messageFormat = "no match in {0} ms: {1}"; //$NON-NLS-1$
        String message = MessageFormat.format(messageFormat, (System.currentTimeMillis() - start), dateTimeString);
        log.trace(message);

        messageFormat = Messages.getString("LenientDateTimeParser.UnknownDateFormat"); //$NON-NLS-1$
        message = MessageFormat.format(messageFormat, dateTimeString);
        throw new ParseException(message, 0);
    }

    /**
     * Like {@link #parseExtended(String, boolean, boolean)} but simply returns
     * the Calendar inside the {@link LenientDateTimeResult} instead of the
     * entire result.
     *
     * @param dateTimeString
     *        the date string to parse (not null).
     * @param defaultDate
     *        if true and the given date time string matches a time-only
     *        pattern, today's date is used in the returned Calendar. If false
     *        and a time-only pattern is matched, 1970/01/01 is used for the
     *        date. See the method Javadoc for more information.
     * @param defaultTime
     *        if true and the given date time string matches a date-only
     *        pattern, the current time is used in the returned Calendar. If
     *        false and a date-only pattern is matched, 00:00:00 is used for the
     *        date. See the method Javadoc for more information.
     * @return a new Calendar containing the date/time that was in the given
     *         dateTimeString.
     * @throws ParseException
     *         if the date time string could not be parsed.
     */
    public Calendar parse(final String dateTimeString, final boolean defaultDate, final boolean defaultTime)
        throws ParseException {
        return parseExtended(dateTimeString, defaultDate, defaultTime).getCalendar();
    }

    /**
     * Like calling {@link #parseExtended(String, boolean, boolean)} with
     * defaultDate and defaultTime set to true but simply returns the Calendar
     * inside the {@link LenientDateTimeResult} instead of the entire result.
     *
     * @see LenientDateTimeParser#parse(String, boolean, boolean)
     *
     * @param dateTimeString
     *        the date string to parse (not null).
     * @return a new Calendar containing the date/time that was in the given
     *         dateTimeString.
     * @throws ParseException
     *         if the date time string could not be parsed.
     */
    public Calendar parse(final String dateTimeString) throws ParseException {
        return parseExtended(dateTimeString, true, true).getCalendar();
    }

    /**
     * Looks for common time zone strings at the end of the given dateTimeString
     * that {@link SimpleDateFormat} can't parse and converts them to equivalent
     * ones it can. For example, "Z" is turned into "UTC".
     *
     * @param dateTimeString
     *        the date time string to parse. If null, null is returned.
     * @return the given dateTimeString with time zones converted to ones
     *         {@link SimpleDateFormat} can parse. Null if the given string was
     *         null.
     */
    protected String convertUnsupportedTimeZones(final String dateTimeString) {
        if (dateTimeString == null) {
            return null;
        }

        /*
         * Convert additional UTC zone strings (like "Z" and "z") to "UTC". We
         * consider the dateTimeString to contain a matching zone string if it
         * ends with it, and the character before it is whitespace or numeric.
         * This lets a date time string like "2007-05-02T22:04:12Foobaz" pass
         * unaltered, because "Foobaz" refers to a (hypothetical) time zone name
         * which SimpleDateFormat could parse.
         */
        for (int i = 0; i < additionalUTCTimeZoneStrings.length; i++) {
            final String zoneString = additionalUTCTimeZoneStrings[i];

            if (dateTimeString.endsWith(zoneString)) {
                final int zoneIndex = dateTimeString.lastIndexOf(zoneString);

                // Make sure there's at least one character before the index.
                if (zoneIndex < 1) {
                    continue;
                }

                final char previousChracter = dateTimeString.charAt(zoneIndex - 1);

                if (Character.isDigit(previousChracter) || Character.isWhitespace(previousChracter)) {
                    // It's a match, replace the string anchored at the end with
                    // UTC.
                    return dateTimeString.replaceAll(zoneString + "$", "UTC"); //$NON-NLS-1$ //$NON-NLS-2$
                }
            }
        }

        return dateTimeString;
    }

    private void computeFormats() {
        /*
         * The expander takes pattern strings (and expands them) or takes
         * already created DateFormat instances and keeps them in the correct
         * order with the expanded strings.
         */
        final LenientDateTimeParserExpander expander = new LenientDateTimeParserExpander(false, locale);

        /*
         * Fill in the default locale date time formats. The indexes (0,1,2,3)
         * correspond to SimpleDateFormat.FULL, LONG, MEDIUM, and SHORT. We go
         * from FULL (0) to MEDIUM (2) with dates to prevent 2-digit date
         * matching (matches way too early). We go from FULL (0) to SHORT (3)
         * with times.
         *
         * Using the integers directly is a hack, but this whole parser is a
         * hack, so I don't feel too bad.
         */
        for (int i = 0; i <= 2; i++) {
            for (int j = 0; j <= 3; j++) {
                final DateFormat df = SimpleDateFormat.getDateTimeInstance(i, j, locale);
                df.setLenient(false);
                expander.add(df, true, true);
            }
        }

        /*
         * We can detect whether this parser's configured locale likes day
         * before month or month before day in its strings by instantiating a
         * short date formatter, passing a different month and day, and seeing
         * which appears first. We use the short date formatter so we don't get
         * (possibly localized) month names.
         */

        final Calendar c = new GregorianCalendar(timeZone, locale);
        c.clear();
        c.set(Calendar.MONTH, Calendar.JANUARY);
        c.set(Calendar.DATE, 5);
        c.set(Calendar.YEAR, 9999);
        final String defaultFormattedDate =
            SimpleDateFormat.getDateInstance(SimpleDateFormat.SHORT, locale).format(c.getTime());

        final int monthIndex = defaultFormattedDate.indexOf("1"); //$NON-NLS-1$
        final int dateIndex = defaultFormattedDate.indexOf("5"); //$NON-NLS-1$

        // ISO goes next.
        expander.addExpanded(isoDateTimeFormats);

        if (dateIndex > monthIndex) {
            /*
             * Month-before-day. Use US before EU.
             */
            expander.addExpanded(usDateFormats);
            expander.addExpanded(euDateFormats);
        } else {
            /*
             * Day-before-month. Use EU before US.
             */
            expander.addExpanded(euDateFormats);
            expander.addExpanded(usDateFormats);
        }

        // Add just the default local date instances.
        for (int i = 0; i <= 2; i++) {
            final DateFormat df = SimpleDateFormat.getDateInstance(i, locale);
            df.setLenient(false);
            expander.add(df, true, false);
        }

        // Add just the default local time instances.
        for (int i = 0; i <= 3; i++) {
            final DateFormat df = SimpleDateFormat.getTimeInstance(i, locale);
            df.setLenient(false);
            expander.add(df, false, true);
        }

        // Generic time goes last.
        expander.addExpanded(genericTimeFormats);

        expandedFormats = expander.getSortedResults();
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return locale.toString() + "@" + timeZone.getID() + " [" + expandedFormats.length + " formats]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    }
}
