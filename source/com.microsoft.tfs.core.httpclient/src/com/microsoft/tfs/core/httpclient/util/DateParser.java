/*
 * $Header:
 * /home/jerenkrantz/tmp/commons/commons-convert/cvs/home/cvs/jakarta-commons
 * //httpclient/src/java/org/apache/commons/httpclient/util/DateParser.java,v
 * 1.11 2004/11/06 19:15:42 mbecke Exp $ $Revision: 480424 $ $Date: 2006-11-29
 * 06:56:49 +0100 (Wed, 29 Nov 2006) $
 *
 * ====================================================================
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many individuals on
 * behalf of the Apache Software Foundation. For more information on the Apache
 * Software Foundation, please see <http://www.apache.org/>.
 */

package com.microsoft.tfs.core.httpclient.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;
import java.util.TimeZone;

/**
 * A utility class for parsing HTTP dates as used in cookies and other headers.
 * This class handles dates as defined by RFC 2616 section 3.3.1 as well as some
 * other common non-standard formats.
 *
 * @author Christopher Brown
 * @author Michael Becke
 *
 * @deprecated Use {@link com.microsoft.tfs.core.httpclient.util.DateUtil}
 */
@Deprecated
public class DateParser {

    /**
     * Date format pattern used to parse HTTP date headers in RFC 1123 format.
     */
    public static final String PATTERN_RFC1123 = "EEE, dd MMM yyyy HH:mm:ss zzz";

    /**
     * Date format pattern used to parse HTTP date headers in RFC 1036 format.
     */
    public static final String PATTERN_RFC1036 = "EEEE, dd-MMM-yy HH:mm:ss zzz";

    /**
     * Date format pattern used to parse HTTP date headers in ANSI C
     * <code>asctime()</code> format.
     */
    public static final String PATTERN_ASCTIME = "EEE MMM d HH:mm:ss yyyy";

    private static final Collection DEFAULT_PATTERNS = Arrays.asList(new String[] {
        PATTERN_ASCTIME,
        PATTERN_RFC1036,
        PATTERN_RFC1123
    });

    /**
     * Parses a date value. The formats used for parsing the date value are
     * retrieved from the default http params.
     *
     * @param dateValue
     *        the date value to parse
     *
     * @return the parsed date
     *
     * @throws DateParseException
     *         if the value could not be parsed using any of the supported date
     *         formats
     */
    public static Date parseDate(final String dateValue) throws DateParseException {
        return parseDate(dateValue, null);
    }

    /**
     * Parses the date value using the given date formats.
     *
     * @param dateValue
     *        the date value to parse
     * @param dateFormats
     *        the date formats to use
     *
     * @return the parsed date
     *
     * @throws DateParseException
     *         if none of the dataFormats could parse the dateValue
     */
    public static Date parseDate(String dateValue, Collection dateFormats) throws DateParseException {

        if (dateValue == null) {
            throw new IllegalArgumentException("dateValue is null");
        }
        if (dateFormats == null) {
            dateFormats = DEFAULT_PATTERNS;
        }
        // trim single quotes around date if present
        // see issue #5279
        if (dateValue.length() > 1 && dateValue.startsWith("'") && dateValue.endsWith("'")) {
            dateValue = dateValue.substring(1, dateValue.length() - 1);
        }

        SimpleDateFormat dateParser = null;
        final Iterator formatIter = dateFormats.iterator();

        while (formatIter.hasNext()) {
            final String format = (String) formatIter.next();
            if (dateParser == null) {
                dateParser = new SimpleDateFormat(format, Locale.US);
                dateParser.setTimeZone(TimeZone.getTimeZone("GMT"));
            } else {
                dateParser.applyPattern(format);
            }
            try {
                return dateParser.parse(dateValue);
            } catch (final ParseException pe) {
                // ignore this exception, we will try the next format
            }
        }

        // we were unable to parse the date
        throw new DateParseException("Unable to parse the date " + dateValue);
    }

    /** This class should not be instantiated. */
    private DateParser() {
    }

}
