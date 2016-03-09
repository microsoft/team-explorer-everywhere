// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.util.datetime;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

import junit.framework.TestCase;

/**
 * WARNING: GMT doesn't parse as a time zone in dates on my platform (JDK 1.5,
 * Linux). GMT does work as a string argument to the TimeZone class constructor.
 * Use UTC when writing test cases that are parsed with LenientDateParser.
 */
public class LenientDateTimeParserTest extends TestCase {
    private LenientDateTimeParser parser;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    public void testConstructionSpeed() throws Exception {
        /*
         * Prints speeds. This test can't fail, but it prints a little reminder
         * when we run it.
         */
        long start = System.currentTimeMillis();
        parser = new LenientDateTimeParser(TimeZone.getDefault(), Locale.US);
        parser.parse("1/2/2007"); //$NON-NLS-1$
        System.out.println("Construction of parser " //$NON-NLS-1$
            + parser.toString()
            + ": " //$NON-NLS-1$
            + (System.currentTimeMillis() - start)
            + " ms"); //$NON-NLS-1$

        start = System.currentTimeMillis();
        parser = new LenientDateTimeParser(TimeZone.getDefault(), Locale.US);
        parser.parse("1/2/2007"); //$NON-NLS-1$
        System.out.println("Reconstruction of parser " //$NON-NLS-1$
            + parser.toString()
            + ": " //$NON-NLS-1$
            + (System.currentTimeMillis() - start)
            + " ms"); //$NON-NLS-1$

        /*
         * Print average parse times for several sample sizes, each an order of
         * magnitude larger than the last.
         */
        for (int a = 0; a < 4; a++) {
            final int sample = (int) Math.pow(10, a);

            long accum = 0;
            for (int i = 0; i < sample; i++) {
                start = System.currentTimeMillis();
                parser.parse("1/2/2007"); //$NON-NLS-1$
                accum += (System.currentTimeMillis() - start);
            }

            System.out.println("Average date parse (for batch " //$NON-NLS-1$
                + sample
                + "):  " //$NON-NLS-1$
                + ((float) accum / (float) sample)
                + " ms"); //$NON-NLS-1$
        }
    }

    public void testInvalidDates() {
        parser = new LenientDateTimeParser(TimeZone.getDefault(), Locale.US);

        try {
            parser.parse(""); //$NON-NLS-1$
            assertFalse("The date should not parse!", false); //$NON-NLS-1$
        } catch (final ParseException e) {
        }

        try {
            parser.parse("fun"); //$NON-NLS-1$
            assertFalse("The date should not parse!", false); //$NON-NLS-1$
        } catch (final ParseException e) {
        }

        try {
            parser.parse("///"); //$NON-NLS-1$
            assertFalse("The date should not parse!", false); //$NON-NLS-1$
        } catch (final ParseException e) {
        }

        try {
            parser.parse(":"); //$NON-NLS-1$
            assertFalse("The date should not parse!", false); //$NON-NLS-1$
        } catch (final ParseException e) {
        }
    }

    public void testExtendedParse() throws Exception {
        /*
         * parseExtended() gives us some more info. Just test the basics.
         */
        parser = new LenientDateTimeParser(TimeZone.getDefault(), Locale.US);

        LenientDateTimeResult result = parser.parseExtended("2/9/2006 14:42:00", true, true); //$NON-NLS-1$
        assertNotNull(result.getMatchedDateFormat());
        assertTrue(result.matchedDate() == true);
        assertTrue(result.matchedTime() == true);

        result = parser.parseExtended("2/9/2006", true, true); //$NON-NLS-1$
        assertNotNull(result.getMatchedDateFormat());
        assertTrue(result.matchedDate() == true);
        assertTrue(result.matchedTime() == false);

        result = parser.parseExtended("14:42:00", true, true); //$NON-NLS-1$
        assertNotNull(result.getMatchedDateFormat());
        assertTrue(result.matchedDate() == false);
        assertTrue(result.matchedTime() == true);
    }

    public void testISODates() throws Exception {
        /*
         * These ISO dates should parse in all locales. The effectiveness of
         * this test procedure depends on the platform's locale support.
         *
         * A small gotcha: on Linux, Sun JDK 1.5, all the Arabic locales fail to
         * parse all dates, so we just skip those.
         *
         * Also, I don't think Thai dates are parsing, because they have weird
         * years, so we skip those. Japan's imperial calendar also uses
         * different years.
         */
        final Locale[] availableLocales = SimpleDateFormat.getAvailableLocales();

        for (int i = 0; i < availableLocales.length; i++) {
            if (availableLocales[i].getLanguage().startsWith("ar") //$NON-NLS-1$
                || availableLocales[i].getLanguage().startsWith("th") //$NON-NLS-1$
                || availableLocales[i].getLanguage().startsWith("ja")) { //$NON-NLS-1$
                continue;
            }

            parser = new LenientDateTimeParser(TimeZone.getDefault(), availableLocales[i]);

            // Date and time.

            check(
                2006,
                Calendar.FEBRUARY,
                28,
                15,
                22,
                9,
                0,
                TimeZone.getDefault(),
                parser.parse("2006-02-28T15:22:09", false, false)); //$NON-NLS-1$

            // Date and time with time zone and lots of precision.

            check(
                2006,
                Calendar.FEBRUARY,
                28,
                15,
                22,
                9,
                0,
                TimeZone.getTimeZone("UTC"), //$NON-NLS-1$
                parser.parse("2006-02-28T15:22:09UTC", false, false)); //$NON-NLS-1$
            check(
                2006,
                Calendar.FEBRUARY,
                28,
                15,
                22,
                9,
                0,
                TimeZone.getTimeZone("UTC-1:00"), //$NON-NLS-1$
                parser.parse("2006-02-28T15:22:09UTC-1:00", false, false)); //$NON-NLS-1$
            check(
                2006,
                Calendar.FEBRUARY,
                28,
                15,
                22,
                9,
                666,
                TimeZone.getTimeZone("UTC-1:00"), //$NON-NLS-1$
                parser.parse("2006-02-28T15:22:09.666UTC-1:00", false, false)); //$NON-NLS-1$

            // Date and time with time zones SimpleDateFormat doesn't know (but
            // we convert).

            check(
                2006,
                Calendar.FEBRUARY,
                28,
                15,
                22,
                9,
                0,
                TimeZone.getTimeZone("UTC"), //$NON-NLS-1$
                parser.parse("2006-02-28T15:22:09Z", false, false)); //$NON-NLS-1$
            check(
                2006,
                Calendar.FEBRUARY,
                28,
                15,
                22,
                9,
                0,
                TimeZone.getTimeZone("UTC"), //$NON-NLS-1$
                parser.parse("2006-02-28T15:22:09z", false, false)); //$NON-NLS-1$
            check(
                2006,
                Calendar.FEBRUARY,
                28,
                15,
                22,
                9,
                0,
                TimeZone.getTimeZone("UTC"), //$NON-NLS-1$
                parser.parse("2006-02-28T15:22:09 Z", false, false)); //$NON-NLS-1$
            check(
                2006,
                Calendar.FEBRUARY,
                28,
                15,
                22,
                9,
                0,
                TimeZone.getTimeZone("UTC"), //$NON-NLS-1$
                parser.parse("2006-02-28T15:22:09 z", false, false)); //$NON-NLS-1$

            // And a case where we shouldn't convert because it could be a real
            // time zone. These should come back in the default time zone.

            check(
                2006,
                Calendar.FEBRUARY,
                28,
                15,
                22,
                9,
                0,
                TimeZone.getDefault(),
                parser.parse("2006-02-28T15:22:09Funbaz", false, false)); //$NON-NLS-1$
            check(
                2006,
                Calendar.FEBRUARY,
                28,
                15,
                22,
                9,
                0,
                TimeZone.getDefault(),
                parser.parse("2006-02-28T15:22:09 XYZ", false, false)); //$NON-NLS-1$
        }
    }

    public void testUSDates() throws Exception {
        parser = new LenientDateTimeParser(TimeZone.getDefault(), Locale.US);

        // Simple date.

        check(2006, Calendar.FEBRUARY, 9, 0, 0, 0, 0, TimeZone.getDefault(), parser.parse("2/9/2006", false, false)); //$NON-NLS-1$
        check(2006, Calendar.FEBRUARY, 9, 0, 0, 0, 0, TimeZone.getDefault(), parser.parse("2/09/2006", false, false)); //$NON-NLS-1$
        check(2006, Calendar.FEBRUARY, 9, 0, 0, 0, 0, TimeZone.getDefault(), parser.parse("02/09/2006", false, false)); //$NON-NLS-1$
        check(2006, Calendar.FEBRUARY, 9, 0, 0, 0, 0, TimeZone.getDefault(), parser.parse("02/9/2006", false, false)); //$NON-NLS-1$

        /*
         * Simple date with current time. We can't actually test for "now"
         * because the parser's "now" was a few nanoseconds ago, so we pass if
         * time isn't exactly midnight (00:00:00.00)
         */
        final Calendar ret = parser.parse("2/9/2006", false, true); //$NON-NLS-1$
        assertTrue(
            (ret.get(Calendar.HOUR) == 0 && ret.get(Calendar.MINUTE) == 0 && ret.get(Calendar.SECOND) == 0) == false);

        // Simple date with two digit year.

        check(2006, Calendar.FEBRUARY, 9, 0, 0, 0, 0, TimeZone.getDefault(), parser.parse(
            "2/9/06", //$NON-NLS-1$
            false,
            false));
        check(2006, Calendar.FEBRUARY, 9, 0, 0, 0, 0, TimeZone.getDefault(), parser.parse("2/09/06", false, false)); //$NON-NLS-1$
        check(2006, Calendar.FEBRUARY, 9, 0, 0, 0, 0, TimeZone.getDefault(), parser.parse("02/09/06", false, false)); //$NON-NLS-1$
        check(2006, Calendar.FEBRUARY, 9, 0, 0, 0, 0, TimeZone.getDefault(), parser.parse("02/9/06", false, false)); //$NON-NLS-1$

        // Long month date.

        check(2006, Calendar.FEBRUARY, 9, 0, 0, 0, 0, TimeZone.getDefault(), parser.parse("Feb/9/2006", false, false)); //$NON-NLS-1$
        check(2006, Calendar.FEBRUARY, 9, 0, 0, 0, 0, TimeZone.getDefault(), parser.parse("FEB/9/2006", false, false)); //$NON-NLS-1$
        check(
            2006,
            Calendar.FEBRUARY,
            9,
            0,
            0,
            0,
            0,
            TimeZone.getDefault(),
            parser.parse("February/9/2006", false, false)); //$NON-NLS-1$

        // Long month date with two digit years.

        check(2006, Calendar.FEBRUARY, 9, 0, 0, 0, 0, TimeZone.getDefault(), parser.parse("Feb/9/06", false, false)); //$NON-NLS-1$
        check(2006, Calendar.FEBRUARY, 9, 0, 0, 0, 0, TimeZone.getDefault(), parser.parse("FEB/9/06", false, false)); //$NON-NLS-1$
        check(
            2006,
            Calendar.FEBRUARY,
            9,
            0,
            0,
            0,
            0,
            TimeZone.getDefault(),
            parser.parse("February/9/06", false, false)); //$NON-NLS-1$

        // Simple date with multiple separators.

        check(2006, Calendar.FEBRUARY, 9, 0, 0, 0, 0, TimeZone.getDefault(), parser.parse("2/9/2006", false, false)); //$NON-NLS-1$
        check(2006, Calendar.FEBRUARY, 9, 0, 0, 0, 0, TimeZone.getDefault(), parser.parse("2-9-2006", false, false)); //$NON-NLS-1$
        check(2006, Calendar.FEBRUARY, 9, 0, 0, 0, 0, TimeZone.getDefault(), parser.parse("2.9.2006", false, false)); //$NON-NLS-1$

        // Date with ambiguous month/date value. US format is preferred.

        check(2006, Calendar.FEBRUARY, 3, 0, 0, 0, 0, TimeZone.getDefault(), parser.parse("2/3/2006", false, false)); //$NON-NLS-1$
        check(2006, Calendar.FEBRUARY, 3, 0, 0, 0, 0, TimeZone.getDefault(), parser.parse(
            "2/3/06", //$NON-NLS-1$
            false,
            false));

        // Date with ambiguous month/date value and multiple separators.

        check(2006, Calendar.FEBRUARY, 3, 0, 0, 0, 0, TimeZone.getDefault(), parser.parse("2/3/2006", false, false)); //$NON-NLS-1$
        check(2006, Calendar.FEBRUARY, 3, 0, 0, 0, 0, TimeZone.getDefault(), parser.parse("2-3-2006", false, false)); //$NON-NLS-1$
        check(2006, Calendar.FEBRUARY, 3, 0, 0, 0, 0, TimeZone.getDefault(), parser.parse("2.3.2006", false, false)); //$NON-NLS-1$

        // Date and time (12-hour clock).

        check(
            2006,
            Calendar.FEBRUARY,
            9,
            1,
            54,
            0,
            0,
            TimeZone.getDefault(),
            parser.parse("2/9/2006 1:54", false, false)); //$NON-NLS-1$
        check(
            2006,
            Calendar.FEBRUARY,
            9,
            12,
            54,
            0,
            0,
            TimeZone.getDefault(),
            parser.parse("2/9/2006 12:54 PM", false, false)); //$NON-NLS-1$
        check(
            2006,
            Calendar.FEBRUARY,
            9,
            12,
            54,
            19,
            0,
            TimeZone.getDefault(),
            parser.parse("2/9/2006 12:54:19 PM", false, false)); //$NON-NLS-1$
        check(
            2006,
            Calendar.FEBRUARY,
            9,
            12,
            54,
            19,
            0,
            TimeZone.getDefault(),
            parser.parse("2/9/2006 12:54:19", false, false)); //$NON-NLS-1$

        // Date and time (24-hour clock).

        check(
            2006,
            Calendar.FEBRUARY,
            9,
            12,
            54,
            0,
            0,
            TimeZone.getDefault(),
            parser.parse("2/9/2006 12:54", false, false)); //$NON-NLS-1$
        check(
            2006,
            Calendar.FEBRUARY,
            9,
            13,
            54,
            19,
            0,
            TimeZone.getDefault(),
            parser.parse("2/9/2006 13:54:19", false, false)); //$NON-NLS-1$
        check(
            2006,
            Calendar.FEBRUARY,
            9,
            1,
            54,
            19,
            0,
            TimeZone.getDefault(),
            parser.parse("2/9/2006 01:54:19", false, false)); //$NON-NLS-1$
        check(
            2006,
            Calendar.FEBRUARY,
            9,
            23,
            54,
            19,
            0,
            TimeZone.getDefault(),
            parser.parse("2/9/2006 23:54:19", false, false)); //$NON-NLS-1$

        // Time only (12-hour clock). Date should default to today.

        final Calendar today = Calendar.getInstance();

        check(
            today.get(Calendar.YEAR),
            today.get(Calendar.MONTH),
            today.get(Calendar.DATE),
            1,
            54,
            0,
            0,
            TimeZone.getDefault(),
            parser.parse("1:54", true, false)); //$NON-NLS-1$
        check(
            today.get(Calendar.YEAR),
            today.get(Calendar.MONTH),
            today.get(Calendar.DATE),
            12,
            54,
            0,
            0,
            TimeZone.getDefault(),
            parser.parse("12:54 PM", true, false)); //$NON-NLS-1$
        check(
            today.get(Calendar.YEAR),
            today.get(Calendar.MONTH),
            today.get(Calendar.DATE),
            12,
            54,
            19,
            0,
            TimeZone.getDefault(),
            parser.parse("12:54:19 pm", true, false)); //$NON-NLS-1$
        check(
            today.get(Calendar.YEAR),
            today.get(Calendar.MONTH),
            today.get(Calendar.DATE),
            12,
            54,
            19,
            0,
            TimeZone.getDefault(),
            parser.parse("12:54:19", true, false)); //$NON-NLS-1$

        // Time only (24-hour clock). Date should default to today.

        check(
            today.get(Calendar.YEAR),
            today.get(Calendar.MONTH),
            today.get(Calendar.DATE),
            12,
            54,
            0,
            0,
            TimeZone.getDefault(),
            parser.parse("12:54", true, false)); //$NON-NLS-1$
        check(
            today.get(Calendar.YEAR),
            today.get(Calendar.MONTH),
            today.get(Calendar.DATE),
            13,
            54,
            19,
            0,
            TimeZone.getDefault(),
            parser.parse("13:54:19", true, false)); //$NON-NLS-1$
        check(
            today.get(Calendar.YEAR),
            today.get(Calendar.MONTH),
            today.get(Calendar.DATE),
            1,
            54,
            19,
            0,
            TimeZone.getDefault(),
            parser.parse("01:54:19", true, false)); //$NON-NLS-1$
        check(
            today.get(Calendar.YEAR),
            today.get(Calendar.MONTH),
            today.get(Calendar.DATE),
            23,
            54,
            19,
            0,
            TimeZone.getDefault(),
            parser.parse("23:54:19", true, false)); //$NON-NLS-1$

        // Time only (12-hour clock). Date should NOT default to today.

        check(1970, 0, 1, 1, 54, 0, 0, TimeZone.getDefault(), parser.parse("1:54", false, false)); //$NON-NLS-1$
        check(1970, 0, 1, 12, 54, 0, 0, TimeZone.getDefault(), parser.parse(
            "12:54 PM", //$NON-NLS-1$
            false,
            false));
        check(1970, 0, 1, 12, 54, 19, 0, TimeZone.getDefault(), parser.parse(
            "12:54:19 pm", //$NON-NLS-1$
            false,
            false));
        check(1970, 0, 1, 12, 54, 19, 0, TimeZone.getDefault(), parser.parse(
            "12:54:19", //$NON-NLS-1$
            false,
            false));

        // Time only (24-hour clock). Date should NOT default to today.

        check(1970, 0, 1, 12, 54, 0, 0, TimeZone.getDefault(), parser.parse("12:54", false, false)); //$NON-NLS-1$
        check(1970, 0, 1, 13, 54, 19, 0, TimeZone.getDefault(), parser.parse(
            "13:54:19", //$NON-NLS-1$
            false,
            false));
        check(1970, 0, 1, 1, 54, 19, 0, TimeZone.getDefault(), parser.parse(
            "01:54:19", //$NON-NLS-1$
            false,
            false));
        check(1970, 0, 1, 23, 54, 19, 0, TimeZone.getDefault(), parser.parse(
            "23:54:19", //$NON-NLS-1$
            false,
            false));

        // Miscellaneous forms that should work in the US locale.

        check(
            2006,
            Calendar.FEBRUARY,
            9,
            0,
            0,
            0,
            0,
            TimeZone.getDefault(),
            parser.parse("February 9, 2006", false, false)); //$NON-NLS-1$
        check(
            2006,
            Calendar.FEBRUARY,
            9,
            23,
            54,
            19,
            0,
            TimeZone.getDefault(),
            parser.parse("February 9, 2006 11:54:19 PM", false, false)); //$NON-NLS-1$
    }

    public void testEuropeDates() throws Exception {
        final Locale[] availableLocales = new Locale[] {
            Locale.FRANCE,
            Locale.GERMAN,
            Locale.ITALY,
            Locale.UK
        };

        for (int i = 0; i < availableLocales.length; i++) {
            parser = new LenientDateTimeParser(TimeZone.getDefault(), availableLocales[i]);

            // Simple date.

            check(
                2006,
                Calendar.FEBRUARY,
                9,
                0,
                0,
                0,
                0,
                TimeZone.getDefault(),
                parser.parse("9/2/2006", false, false)); //$NON-NLS-1$
            check(
                2006,
                Calendar.FEBRUARY,
                9,
                0,
                0,
                0,
                0,
                TimeZone.getDefault(),
                parser.parse("09/2/2006", false, false)); //$NON-NLS-1$
            check(
                2006,
                Calendar.FEBRUARY,
                9,
                0,
                0,
                0,
                0,
                TimeZone.getDefault(),
                parser.parse("09/02/2006", false, false)); //$NON-NLS-1$
            check(
                2006,
                Calendar.FEBRUARY,
                9,
                0,
                0,
                0,
                0,
                TimeZone.getDefault(),
                parser.parse("09/2/2006", false, false)); //$NON-NLS-1$

            /*
             * Simple date with current time. We can't actually test for "now"
             * because the parser's "now" was a few nanoseconds ago, so we pass
             * if time isn't exactly midnight (00:00:00.00)
             */
            final Calendar ret = parser.parse("2/9/2006", false, true); //$NON-NLS-1$
            assertTrue(
                (ret.get(Calendar.HOUR) == 0
                    && ret.get(Calendar.MINUTE) == 0
                    && ret.get(Calendar.SECOND) == 0) == false);

            // Simple date with two digit year.

            check(2006, Calendar.FEBRUARY, 9, 0, 0, 0, 0, TimeZone.getDefault(), parser.parse("9/2/06", false, false)); //$NON-NLS-1$
            check(2006, Calendar.FEBRUARY, 9, 0, 0, 0, 0, TimeZone.getDefault(), parser.parse("09/2/06", false, false)); //$NON-NLS-1$
            check(
                2006,
                Calendar.FEBRUARY,
                9,
                0,
                0,
                0,
                0,
                TimeZone.getDefault(),
                parser.parse("09/02/06", false, false)); //$NON-NLS-1$
            check(2006, Calendar.FEBRUARY, 9, 0, 0, 0, 0, TimeZone.getDefault(), parser.parse("09/2/06", false, false)); //$NON-NLS-1$

            final String[] monthNames = new String[2];

            if (availableLocales[i].equals(Locale.UK)) {
                monthNames[0] = "Feb"; //$NON-NLS-1$
                monthNames[1] = "February"; //$NON-NLS-1$
            } else if (availableLocales[i].equals(Locale.FRANCE)) {
                /*
                 * Unicode 0x00E9 is small e with acute (é). Better to escape it
                 * because sometimes Eclipse detects this file's encoding wrong.
                 */
                monthNames[0] = "F\u00E9vr."; //$NON-NLS-1$
                monthNames[1] = "F\u00E9vrier"; //$NON-NLS-1$
            } else if (availableLocales[i].equals(Locale.GERMAN)) {
                monthNames[0] = "Feb"; //$NON-NLS-1$
                monthNames[1] = "Februar"; //$NON-NLS-1$
            } else if (availableLocales[i].equals(Locale.ITALY)) {
                monthNames[0] = "Feb"; //$NON-NLS-1$
                monthNames[1] = "Febbraio"; //$NON-NLS-1$
            }

            // Iterate through short and long month.
            for (int j = 0; j < 2; j++) {
                // Long month date.

                check(
                    2006,
                    Calendar.FEBRUARY,
                    9,
                    0,
                    0,
                    0,
                    0,
                    TimeZone.getDefault(),
                    parser.parse("9/" + monthNames[j] + "/2006", false, false)); //$NON-NLS-1$ //$NON-NLS-2$
                check(
                    2006,
                    Calendar.FEBRUARY,
                    9,
                    0,
                    0,
                    0,
                    0,
                    TimeZone.getDefault(),
                    parser.parse("9/" + monthNames[j].toUpperCase() + "/2006", false, false)); //$NON-NLS-1$ //$NON-NLS-2$
                check(
                    2006,
                    Calendar.FEBRUARY,
                    9,
                    0,
                    0,
                    0,
                    0,
                    TimeZone.getDefault(),
                    parser.parse("9/" + monthNames[j].toLowerCase() + "/2006", false, false)); //$NON-NLS-1$ //$NON-NLS-2$

                // Long month date with two digit years.

                check(
                    2006,
                    Calendar.FEBRUARY,
                    9,
                    0,
                    0,
                    0,
                    0,
                    TimeZone.getDefault(),
                    parser.parse("9/" + monthNames[j] + "/06", false, false)); //$NON-NLS-1$ //$NON-NLS-2$
                check(
                    2006,
                    Calendar.FEBRUARY,
                    9,
                    0,
                    0,
                    0,
                    0,
                    TimeZone.getDefault(),
                    parser.parse("9/" + monthNames[j].toUpperCase() + "/06", false, false)); //$NON-NLS-1$ //$NON-NLS-2$
                check(
                    2006,
                    Calendar.FEBRUARY,
                    9,
                    0,
                    0,
                    0,
                    0,
                    TimeZone.getDefault(),
                    parser.parse("9/" + monthNames[j].toLowerCase() + "/06", false, false)); //$NON-NLS-1$ //$NON-NLS-2$
            }

            // Simple date with multiple separators.

            check(
                2006,
                Calendar.FEBRUARY,
                9,
                0,
                0,
                0,
                0,
                TimeZone.getDefault(),
                parser.parse("9/2/2006", false, false)); //$NON-NLS-1$
            check(
                2006,
                Calendar.FEBRUARY,
                9,
                0,
                0,
                0,
                0,
                TimeZone.getDefault(),
                parser.parse("9-2-2006", false, false)); //$NON-NLS-1$
            check(
                2006,
                Calendar.FEBRUARY,
                9,
                0,
                0,
                0,
                0,
                TimeZone.getDefault(),
                parser.parse("9.2.2006", false, false)); //$NON-NLS-1$

            // Date with ambiguous month/date value. European format is
            // preferred.

            check(
                2006,
                Calendar.FEBRUARY,
                3,
                0,
                0,
                0,
                0,
                TimeZone.getDefault(),
                parser.parse("3/2/2006", false, false)); //$NON-NLS-1$
            check(2006, Calendar.FEBRUARY, 3, 0, 0, 0, 0, TimeZone.getDefault(), parser.parse("3/2/06", false, false)); //$NON-NLS-1$

            // Date with ambiguous month/date value and multiple separators.

            check(
                2006,
                Calendar.FEBRUARY,
                3,
                0,
                0,
                0,
                0,
                TimeZone.getDefault(),
                parser.parse("3/2/2006", false, false)); //$NON-NLS-1$
            check(
                2006,
                Calendar.FEBRUARY,
                3,
                0,
                0,
                0,
                0,
                TimeZone.getDefault(),
                parser.parse("3-2-2006", false, false)); //$NON-NLS-1$
            check(
                2006,
                Calendar.FEBRUARY,
                3,
                0,
                0,
                0,
                0,
                TimeZone.getDefault(),
                parser.parse("3.2.2006", false, false)); //$NON-NLS-1$

            // Date and time (12-hour clock).

            check(
                2006,
                Calendar.FEBRUARY,
                9,
                1,
                54,
                0,
                0,
                TimeZone.getDefault(),
                parser.parse("9/2/2006 1:54", false, false)); //$NON-NLS-1$
            check(
                2006,
                Calendar.FEBRUARY,
                9,
                12,
                54,
                0,
                0,
                TimeZone.getDefault(),
                parser.parse("9/2/2006 12:54 PM", false, false)); //$NON-NLS-1$
            check(
                2006,
                Calendar.FEBRUARY,
                9,
                12,
                54,
                19,
                0,
                TimeZone.getDefault(),
                parser.parse("9/2/2006 12:54:19 PM", false, false)); //$NON-NLS-1$
            check(
                2006,
                Calendar.FEBRUARY,
                9,
                12,
                54,
                19,
                0,
                TimeZone.getDefault(),
                parser.parse("9/2/2006 12:54:19", false, false)); //$NON-NLS-1$

            // Date and time (24-hour clock).

            check(
                2006,
                Calendar.FEBRUARY,
                9,
                12,
                54,
                0,
                0,
                TimeZone.getDefault(),
                parser.parse("9/2/2006 12:54", false, false)); //$NON-NLS-1$
            check(
                2006,
                Calendar.FEBRUARY,
                9,
                13,
                54,
                19,
                0,
                TimeZone.getDefault(),
                parser.parse("9/2/2006 13:54:19", false, false)); //$NON-NLS-1$
            check(
                2006,
                Calendar.FEBRUARY,
                9,
                1,
                54,
                19,
                0,
                TimeZone.getDefault(),
                parser.parse("9/2/2006 01:54:19", false, false)); //$NON-NLS-1$
            check(
                2006,
                Calendar.FEBRUARY,
                9,
                23,
                54,
                19,
                0,
                TimeZone.getDefault(),
                parser.parse("9/2/2006 23:54:19", false, false)); //$NON-NLS-1$

            // Time only (12-hour clock). Date should default to today.
            final Calendar today = Calendar.getInstance();

            check(
                today.get(Calendar.YEAR),
                today.get(Calendar.MONTH),
                today.get(Calendar.DATE),
                1,
                54,
                0,
                0,
                TimeZone.getDefault(),
                parser.parse("1:54", true, false)); //$NON-NLS-1$
            check(
                today.get(Calendar.YEAR),
                today.get(Calendar.MONTH),
                today.get(Calendar.DATE),
                12,
                54,
                0,
                0,
                TimeZone.getDefault(),
                parser.parse("12:54 PM", true, false)); //$NON-NLS-1$
            check(
                today.get(Calendar.YEAR),
                today.get(Calendar.MONTH),
                today.get(Calendar.DATE),
                12,
                54,
                19,
                0,
                TimeZone.getDefault(),
                parser.parse("12:54:19 pm", true, false)); //$NON-NLS-1$
            check(
                today.get(Calendar.YEAR),
                today.get(Calendar.MONTH),
                today.get(Calendar.DATE),
                12,
                54,
                19,
                0,
                TimeZone.getDefault(),
                parser.parse("12:54:19", true, false)); //$NON-NLS-1$

            // Time only (24-hour clock). Date should default to today.
            check(
                today.get(Calendar.YEAR),
                today.get(Calendar.MONTH),
                today.get(Calendar.DATE),
                12,
                54,
                0,
                0,
                TimeZone.getDefault(),
                parser.parse("12:54", true, false)); //$NON-NLS-1$
            check(
                today.get(Calendar.YEAR),
                today.get(Calendar.MONTH),
                today.get(Calendar.DATE),
                13,
                54,
                19,
                0,
                TimeZone.getDefault(),
                parser.parse("13:54:19", true, false)); //$NON-NLS-1$
            check(
                today.get(Calendar.YEAR),
                today.get(Calendar.MONTH),
                today.get(Calendar.DATE),
                1,
                54,
                19,
                0,
                TimeZone.getDefault(),
                parser.parse("01:54:19", true, false)); //$NON-NLS-1$
            check(
                today.get(Calendar.YEAR),
                today.get(Calendar.MONTH),
                today.get(Calendar.DATE),
                23,
                54,
                19,
                0,
                TimeZone.getDefault(),
                parser.parse("23:54:19", true, false)); //$NON-NLS-1$

            // Time only (12-hour clock). Date should NOT default to today.

            check(1970, 0, 1, 1, 54, 0, 0, TimeZone.getDefault(), parser.parse("1:54", false, false)); //$NON-NLS-1$
            check(1970, 0, 1, 12, 54, 0, 0, TimeZone.getDefault(), parser.parse(
                "12:54 PM", //$NON-NLS-1$
                false,
                false));
            check(1970, 0, 1, 12, 54, 19, 0, TimeZone.getDefault(), parser.parse(
                "12:54:19 pm", //$NON-NLS-1$
                false,
                false));
            check(1970, 0, 1, 12, 54, 19, 0, TimeZone.getDefault(), parser.parse(
                "12:54:19", //$NON-NLS-1$
                false,
                false));

            // Time only (24-hour clock). Date should NOT default to today.

            check(1970, 0, 1, 12, 54, 0, 0, TimeZone.getDefault(), parser.parse(
                "12:54", //$NON-NLS-1$
                false,
                false));
            check(1970, 0, 1, 13, 54, 19, 0, TimeZone.getDefault(), parser.parse(
                "13:54:19", //$NON-NLS-1$
                false,
                false));
            check(1970, 0, 1, 1, 54, 19, 0, TimeZone.getDefault(), parser.parse(
                "01:54:19", //$NON-NLS-1$
                false,
                false));
            check(1970, 0, 1, 23, 54, 19, 0, TimeZone.getDefault(), parser.parse(
                "23:54:19", //$NON-NLS-1$
                false,
                false));

            // Miscellaneous forms that should work in the English-speaking EU
            // locales.

            if (availableLocales[i].getLanguage().startsWith("en")) //$NON-NLS-1$
            {
                check(
                    2006,
                    Calendar.FEBRUARY,
                    9,
                    0,
                    0,
                    0,
                    0,
                    TimeZone.getDefault(),
                    parser.parse("9 Feb 2006", false, false)); //$NON-NLS-1$
                check(
                    2006,
                    Calendar.FEBRUARY,
                    9,
                    0,
                    0,
                    0,
                    0,
                    TimeZone.getDefault(),
                    parser.parse("9 February 2006", false, false)); //$NON-NLS-1$
                check(
                    2006,
                    Calendar.FEBRUARY,
                    9,
                    23,
                    54,
                    19,
                    0,
                    TimeZone.getDefault(),
                    parser.parse("9 February 2006 23:54:19", false, false)); //$NON-NLS-1$
            }
        }
    }

    /**
     * Checks the given parsed Calendar against the specified field values. The
     * given time values are interpreted without any Daylight Saving Time
     * adjustment (they ignore DST entirely).
     *
     * @param year
     *        the year to test the parsed Calendar against (ignoring DST).
     * @param zeroBasedMonth
     *        the zeroBasedMonth to test the parsed Calendar against (ignoring
     *        DST).
     * @param dayOfMonth
     *        the dayOfMonth to test the parsed Calendar against (ignoring DST).
     * @param hour
     *        the hour (0-23) to test the parsed Calendar against (ignoring
     *        DST).
     * @param minute
     *        the minute (0-59) to test the parsed Calendar against (ignoring
     *        DST).
     * @param second
     *        the second (0-59) to test the parsed Calendar against (ignoring
     *        DST).
     * @param millisecond
     *        the millisecond (0-999) to test the parsed Calendar against
     *        (ignoring DST).
     * @param parsed
     *        the parsed Calendar you got from LenientDateParser.parse().
     */
    private void check(
        final int year,
        final int zeroBasedMonth,
        final int dayOfMonth,
        final int hour,
        final int minute,
        final int second,
        final int millisecond,
        final TimeZone zone,
        final Calendar parsed) {
        /*
         * NOTE: We use the Canada locale to construct the reference calendar
         * because it uses a 24-hour clock, which makes these test cases much
         * easier to write (we don't have to remember to put AM/PM in each one).
         */

        final Calendar reference = Calendar.getInstance(zone, Locale.CANADA);
        reference.set(year, zeroBasedMonth, dayOfMonth, hour, minute, second);
        reference.set(Calendar.MILLISECOND, millisecond);

        assertEquals(reference.getTime().getTime(), parsed.getTime().getTime());
    }
}
