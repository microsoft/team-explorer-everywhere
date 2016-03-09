// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.util.datetime;

import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.util.Check;

/**
 *         Static methods to expand lists of date/time formats by all the
 *         possible separator characters, month patterns, and time zone patterns
 *         that might be needed for parsing.
 *
 *         This class is NOT thread-safe for speed.
 */
public class LenientDateTimeParserExpander {
    private static final Log log = LogFactory.getLog(LenientDateTimeParserExpander.class);

    /**
     * Each of these date separators is tried for each known format string.
     */
    private static final String[] dateSeparators = {
        "/", //$NON-NLS-1$
        "-", //$NON-NLS-1$
        "." //$NON-NLS-1$
    };

    /**
     * Each of these time separators is tried for each known format string.
     */
    private static final String[] timeSeparators = {
        ":", //$NON-NLS-1$
        "." //$NON-NLS-1$
    };

    /**
     * Each "MM" instance of a pattern is expanded to each of these.
     */
    private static final String[] monthPatterns = {
        "MM", //$NON-NLS-1$
        "MMM" //$NON-NLS-1$
    };

    /**
     * Each "z" instance of a pattern is expanded to each of these.
     */
    private static final String[] zonePatterns = {
        "z", //$NON-NLS-1$
        "Z" //$NON-NLS-1$
    };

    private final boolean constructLenient;
    private final Set lenientDateTimeParserFormats = new HashSet();
    private final Locale locale;

    private int counter = 0;

    /**
     * @param constructLenient
     *        the value is passed to setLenient() on all newly constructed
     *        DateFormat instances returned (not null).
     */
    protected LenientDateTimeParserExpander(final boolean constructLenient, final Locale locale) {
        this.constructLenient = constructLenient;
        this.locale = locale;
    }

    /**
     * Add a single format to this expander's contents.
     *
     * @param format
     *        the DateFormat string to add (not null).
     */
    public void add(final DateFormat format, final boolean specifiesDate, final boolean specifiesTime) {
        lenientDateTimeParserFormats.add(new LenientDateTimeFormat(format, counter++, specifiesDate, specifiesTime));
    }

    /**
     * Add a single format to this expander's contents.
     *
     * @param pattern
     *        the single pattern string to add (not null).
     */
    public void addExpanded(final String pattern, final boolean specifiesDate, final boolean specifiesTime) {
        final SimpleDateFormat sdf = new SimpleDateFormat(pattern, locale);
        sdf.setLenient(constructLenient);

        lenientDateTimeParserFormats.add(new LenientDateTimeFormat(sdf, counter++, specifiesDate, specifiesTime));
    }

    /**
     * Given an array of strings, expands each into multiple format strings and
     * add the results to this instance in order.
     *
     * The formats are expanded to include variants with all the date
     * separators, time separators, month styles, and zone patterns defined
     * above (_dateSeparators, _timeSeparators, _monthPatterns, _zonePatterns).
     *
     * @param patterns
     *        the patterns to expand (not null).
     *
     */
    public void addExpanded(final LenientDateTimePattern[] patterns) {
        Check.notNull(patterns, "patterns"); //$NON-NLS-1$

        /*
         * This is officially the deepest I've ever nested loops in any program.
         */
        for (int i = 0; i < patterns.length; i++) {
            final LenientDateTimePattern pattern = patterns[i];

            for (int j = 0; j < dateSeparators.length; j++) {
                for (int k = 0; k < timeSeparators.length; k++) {
                    for (int l = 0; l < monthPatterns.length; l++) {
                        for (int m = 0; m < zonePatterns.length; m++) {
                            final SimpleDateFormat sdf =
                                new SimpleDateFormat(pattern.getPattern().replaceAll("/", dateSeparators[j]).replaceAll( //$NON-NLS-1$
                                    ":", //$NON-NLS-1$
                                    timeSeparators[k]).replaceAll("MM", monthPatterns[l]).replaceAll( //$NON-NLS-1$
                                        "z", //$NON-NLS-1$
                                        zonePatterns[m]),
                                    locale);

                            sdf.setLenient(constructLenient);

                            lenientDateTimeParserFormats.add(
                                new LenientDateTimeFormat(
                                    sdf,
                                    counter++,
                                    pattern.specifiesDate(),
                                    pattern.specifiesTime()));
                        }
                    }
                }
            }
        }
    }

    /**
     * Sorts the items in this expander's set and returns a flattened array.
     *
     * @return the expanded items in the order they were added.
     */
    public LenientDateTimeFormat[] getSortedResults() {
        /*
         * Flatten the set into an array.
         */
        final LenientDateTimeFormat[] flat = (LenientDateTimeFormat[]) lenientDateTimeParserFormats.toArray(
            new LenientDateTimeFormat[lenientDateTimeParserFormats.size()]);

        /*
         * Sort the formats in a way that preserved the original order of their
         * definition before expansion.
         */
        Arrays.sort(flat, new LenientDateTimeFormatComparator());

        if (log.isTraceEnabled()) {
            for (int i = 0; i < flat.length; i++) {
                final DateFormat format = flat[i].getDateFormat();

                if (format instanceof SimpleDateFormat) {
                    final String pattern = ((SimpleDateFormat) format).toPattern();
                    final String messageFormat = "expanded format {0}: {1}"; //$NON-NLS-1$
                    final String message = MessageFormat.format(messageFormat, i, pattern);
                    log.trace(message);
                } else {
                    final String messageFormat = "expanded format {0}: {1}"; //$NON-NLS-1$
                    final String message = MessageFormat.format(messageFormat, i, format.toString());
                    log.trace(message);
                }
            }
        }

        return flat;
    }
}
