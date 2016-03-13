// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.ws.runtime.xml;

/*
 * The following applies only to toCalendar(String):
 *
 * Copyright 2001-2004 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

import com.microsoft.tfs.core.ws.runtime.types.GUID;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.StringUtil;
import com.microsoft.tfs.util.base64.Base64;

/**
 * <p>
 * Static utility methods for converting between XML Schema element and
 * attribute string data and Java built-in types. For instance, a Java boolean
 * with value <code>false</code> can be converted to/from the XML string
 * "false". These methods are not for creating or parsing XML structure (see
 * {@link XMLStreamWriterHelper} and {@link XMLStreamReaderHelper}).
 * </p>
 * <p>
 * For speed, the {@link Check} class is not often used to validate parameters
 * to these methods.
 * </p>
 *
 * @threadsafety thread-safe
 */
public abstract class XMLConvert {
    /**
     * These are the XML Schema date formatters, we always parse and format in
     * the US locale. If the locale is not specified, Java will use the default
     * locale, which causes parsing problems when the default locale's date
     * format is too different from SOAP's.
     * <p>
     * Access to these formatters must be synchronized (on the
     * {@link DateFormat} objects themselves). See the Javadoc on
     * {@link SimpleDateFormat} for details.
     */

    /**
     * The UTC DateTime format (e.g. 2011-08-01T19:19:53.574Z)
     */
    private static final DateFormat UTC_DATETIME_FORMAT =
        new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US); //$NON-NLS-1$

    /**
     * The LOCAL DateTime format (e.g. 2011-08-01T15:19:53.574-0400)
     */
    private static final DateFormat LOCAL_DATETIME_FORMAT =
        new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US); //$NON-NLS-1$

    /**
     * The UTC Date format (e.g. 2011-08-01Z)
     */
    private static final DateFormat UTC_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'Z'", Locale.US); //$NON-NLS-1$

    /**
     * The LOCAL DateTime format (e.g. 2011-08-01-0400)
     */
    private static final DateFormat LOCAL_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-ddZ", Locale.US); //$NON-NLS-1$

    /**
     * Cached for efficiency.
     */
    private static final TimeZone UTC_TIME_ZONE = TimeZone.getTimeZone("GMT"); //$NON-NLS-1$

    static {
        synchronized (UTC_DATETIME_FORMAT) {
            UTC_DATETIME_FORMAT.setTimeZone(UTC_TIME_ZONE);
        }
        synchronized (UTC_DATE_FORMAT) {
            UTC_DATE_FORMAT.setTimeZone(UTC_TIME_ZONE);
        }
    }

    /*
     * These methods convert from a primitive or Object type to a String
     * representation.
     */

    /**
     * Converts the given value to the XML string value.
     *
     * @param value
     *        the value to convert.
     * @return the converted string.
     */
    public static String toString(final boolean value) {
        return value ? "true" : "false"; //$NON-NLS-1$ //$NON-NLS-2$
    }

    /**
     * Converts the given value to the XML string value.
     *
     * @param value
     *        the value to convert.
     * @return the converted string.
     */
    public static String toString(final char value) {
        return Character.toString(value);
    }

    /**
     * Converts the given value to the XML string value.
     *
     * @param value
     *        the value to convert.
     * @return the converted string.
     */
    public static String toString(final short value) {
        return Short.toString(value);
    }

    /**
     * Converts the given value to the XML string value.
     *
     * @param value
     *        the value to convert.
     * @return the converted string.
     */
    public static String toString(final int value) {
        return Integer.toString(value);
    }

    /**
     * Converts the given value to the XML string value.
     *
     * @param value
     *        the value to convert.
     * @return the converted string.
     */
    public static String toString(final long value) {
        return Long.toString(value);
    }

    /**
     * Converts the given value to the XML string value.
     *
     * @param value
     *        the value to convert.
     * @return the converted string.
     */
    public static String toString(final float value) {
        return Float.toString(value);
    }

    /**
     * Converts the given value to the XML string value.
     *
     * @param value
     *        the value to convert.
     * @return the converted string.
     */
    public static String toString(final double value) {
        return Double.toString(value);
    }

    /**
     * Converts the given value to an XML Schema DateTime <b>or</b> Date value.
     * <p>
     * The {@link Calendar} is not converted to local time.
     *
     * @param value
     *        the value to convert.
     * @param includeTime
     *        if <code>true</code> the time information in the {@link Calendar}
     *        is included in the string (an XML Schema "DateTime" value is
     *        returned ), if <code>false</code> the time information is omitted
     *        from the string (an XML Schema "Date" value is returned).
     * @return the converted string.
     */
    public static String toString(final Calendar value, final boolean includeTime) {
        return toString(value, includeTime, false);
    }

    /**
     * Converts the given value to an XML Schema DateTime <b>or</b> Date value
     * in either LOCAL or UTC format.
     *
     * @param value
     *        The calendar to convert.
     * @param includeTime
     *        if <code>true</code> the time information in the {@link Calendar}
     *        is included in the string (an XML Schema "DateTime" value is
     *        returned ), if <code>false</code> the time information is omitted
     *        from the string (an XML Schema "Date" value is returned).
     * @param toLocalFormat
     *        True for LOCAL format, otherwise output as UTC format.
     * @return The LOCAL or UTC string format of the Calendar.
     */
    public static String toString(final Calendar value, final boolean includeTime, final boolean toLocalFormat) {
        if (toLocalFormat) {
            final String localFormat;
            if (includeTime) {
                synchronized (LOCAL_DATETIME_FORMAT) {
                    localFormat = LOCAL_DATETIME_FORMAT.format(value.getTime());
                }
            } else {
                synchronized (LOCAL_DATE_FORMAT) {
                    localFormat = LOCAL_DATE_FORMAT.format(value.getTime());
                }
            }

            /*
             * Java's SimpleDateFormat produces a time-zone portion that differs
             * from what XmlConvert produced for DateTime in .NET. .NET includes
             * a ':' in the time-zone and Java does not. We need to add the
             * colon to be compatible with .NET.
             */

            final StringBuffer sb = new StringBuffer();
            sb.append(localFormat.substring(0, localFormat.length() - 2));
            sb.append(":"); //$NON-NLS-1$
            sb.append(localFormat.substring(localFormat.length() - 2, localFormat.length()));

            return sb.toString();
        } else {
            if (includeTime) {
                synchronized (UTC_DATETIME_FORMAT) {
                    return UTC_DATETIME_FORMAT.format(value.getTime());
                }
            } else {
                synchronized (UTC_DATE_FORMAT) {
                    return UTC_DATE_FORMAT.format(value.getTime());
                }
            }
        }
    }

    /**
     * Converts the given value to the XML string value.
     *
     * @param value
     *        the value to convert.
     * @return the converted string.
     */
    public static String toString(final BigDecimal value) {
        return value.toString();
    }

    /**
     * Converts the given value to the XML string value.
     *
     * @param value
     *        the value to convert.
     * @return the converted string.
     */
    public static String toString(final GUID value) {
        return value.toString();
    }

    /*
     * These methods convert from a String representation to a primitive or
     * Object type.
     */

    /**
     * Converts the given XML string to a short value.
     *
     * @param value
     *        the string value to convert.
     * @return the converted value.
     */
    public static short toShort(final String value) {
        return Short.parseShort(value);
    }

    /**
     * Converts the given XML string to an integer value.
     *
     * @param value
     *        the string value to convert.
     * @return the converted value.
     */
    public static int toInt(final String value) {
        return Integer.parseInt(value);
    }

    /**
     * Converts the given XML string to a long value.
     *
     * @param value
     *        the string value to convert.
     * @return the converted value.
     */
    public static long toLong(final String value) {
        if (StringUtil.isNullOrEmpty(value)) {
            return -1;
        } else {
            return Long.parseLong(value);
        }
    }

    /**
     * Converts the given XML string to a float value.
     *
     * @param value
     *        the string value to convert.
     * @return the converted value.
     */
    public static float toFloat(final String value) {
        return Float.parseFloat(value);
    }

    /**
     * Converts the given XML string to a double value.
     *
     * @param value
     *        the string value to convert.
     * @return the converted value.
     */
    public static double toDouble(final String value) {
        return Double.parseDouble(value);
    }

    /**
     * Converts the given XML string to a boolean value.
     *
     * @param value
     *        the string value to convert.
     * @return the converted value.
     */
    public static boolean toBoolean(final String value) {
        return Boolean.valueOf(value).booleanValue();
    }

    /**
     * Converts the given XML string to a byte value.
     *
     * @param value
     *        the string value to convert.
     * @return the converted value.
     */
    public static byte toByte(final String value) {
        return Byte.parseByte(value);
    }

    /**
     * Converts the given XML string to a character value.
     *
     * @param value
     *        the string value to convert.
     * @return the converted value.
     */
    public static char toCharacter(final String value) {
        return value.charAt(0);
    }

    /**
     * Converts the given XML string to a {@link BigDecimal} value.
     *
     * @param value
     *        the string value to convert.
     * @return the converted value.
     */
    public static BigDecimal toBigDecimal(final String value) {
        if (value == null) {
            return null;
        }

        return new BigDecimal(value);
    }

    /**
     * Converts the given XML string to a {@link GUID} value.
     *
     * @param value
     *        the string value to convert.
     * @return the converted value.
     */
    public static GUID toGUID(final String value) {
        if (StringUtil.isNullOrEmpty(value)) {
            return null;
        }

        return new GUID(value);
    }

    /**
     * Converts the given XML string, which is base64-encoded, to a byte[]
     * value.
     *
     * @param value
     *        the string value to convert.
     * @return the converted value, byte[0] if the given string was null.
     */
    public final static byte[] toByteArray(final String value) {
        if (value == null) {
            return new byte[0];
        }

        /*
         * Base64 is defined in terms of characters that are common between
         * ASCII and EBCDIC, so we can force ASCII.
         */
        try {
            return Base64.decodeBase64(value.getBytes("US-ASCII")); //$NON-NLS-1$
        } catch (final UnsupportedEncodingException e) {
            /*
             * Should never happen as all JVMs are required to support US-ASCII.
             */
            throw new RuntimeException(e);
        }
    }

    /**
     * <p>
     * Parses an XML Schema Date or DateTime string value into a
     * {@link Calendar}.
     * </p>
     * <p>
     * The time zone in the string determines the time zone in the parsed
     * {@link Calendar}. See {@link #parseDate(String)} for info on time zones
     * in dates.
     * </p>
     *
     * @param value
     *        the SOAP date or dateTime to parse.
     * @param includeTime
     *        if <code>true</code> the input string is an XML Schema dateTime
     *        (date and time) and the time part is required, if
     *        <code>false</code> the input string is an XML Schema date (no
     *        time)
     * @return the parsed {@link Calendar} value or <code>null</code> if the
     *         input string was <code>null</code> or empty
     */
    public static Calendar toCalendar(final String value, final boolean includeTime) {
        if (StringUtil.isNullOrEmpty(value)) {
            return null;
        }

        if (includeTime) {
            return parseDateTime(value);
        } else {
            return parseDate(value);
        }
    }

    /**
     * <p>
     * Time zones in date strings:
     * </p>
     * <ul>
     * <li>If the string specifies the UTC time zone with the literal "Z", the
     * string refers to a UTC date and the returned {@link Calendar} is set to
     * use the UTC time zone.</li>
     * <li>If the string does not specify a time zone, the string refers to a
     * UTC date but the returned {@link Calendar} is set to use the
     * <i>default</i> time zone.</li>
     * <li>If any other time zone is specified, the string refers to a date in
     * the specified time zone, but the returned {@link Calendar} is set to use
     * the <i>default</i> time zone.
     * </ul>
     * <p>
     * The returned {@link Calendar} refers to the first millisecond of the
     * parsed date (millisecond 0 of second 0 of minute 0 of hour 0). In other
     * words, "2011-09-23Z" yields a {@link Calendar} of
     * "2011-09-26T00:00:00.000Z".
     * </p>
     * <p>
     * Because the returned time zone can be different from the one specified in
     * the string, {@link Calendar} fields like "day-of-month" and "hour" may
     * not match the literal value in the string, though the {@link Calendar}
     * and literal string refer to the same instant.
     * </p>
     * <p>
     *
     * <pre>
     * Copyright 2001-2004 The Apache Software Foundation.
     * Licensed under the Apache License, Version 2.0 (the &quot;License&quot;); you may
     * not use this file except in compliance with the License. You may obtain a
     * copy of the License at
     * http://www.apache.org/licenses/LICENSE-2.0
     * Unless required by applicable law or agreed to in writing, software
     * distributed under the License is distributed on an &quot;AS IS&quot; BASIS, WITHOUT
     * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
     * License for the specific language governing permissions and limitations
     * under the License.
     * </pre>
     *
     * </p>
     *
     * @author Sam Ruby (rubys@us.ibm.com)
     * @author Rich Scheuerle (scheu@us.ibm.com) Modified for JAX-RPC
     */
    public static Calendar parseDate(String value) {
        final Calendar calendar = Calendar.getInstance();
        Date date;
        boolean bc = false;

        // validate fixed portion of format
        if (value == null || value.length() == 0) {
            throw new NumberFormatException("Bad date value"); //$NON-NLS-1$
        }
        if (value.charAt(0) == '+') {
            value = value.substring(1);
        }
        if (value.charAt(0) == '-') {
            value = value.substring(1);
            bc = true;
        }
        if (value.length() < 10) {
            throw new NumberFormatException("Bad date value"); //$NON-NLS-1$
        }
        if (value.charAt(4) != '-' || value.charAt(7) != '-') {
            throw new NumberFormatException("bad date"); //$NON-NLS-1$
        }
        // convert what we have validated so far
        try {
            synchronized (UTC_DATE_FORMAT) {
                date = UTC_DATE_FORMAT.parse(value.substring(0, 10) + "Z"); //$NON-NLS-1$
            }
        } catch (final Exception e) {
            throw new NumberFormatException(e.toString());
        }

        int pos = 10;

        // parse optional timezone
        if (pos + 5 < value.length() && (value.charAt(pos) == '+' || (value.charAt(pos) == '-'))) {
            if (!Character.isDigit(value.charAt(pos + 1))
                || !Character.isDigit(value.charAt(pos + 2))
                || value.charAt(pos + 3) != ':'
                || !Character.isDigit(value.charAt(pos + 4))
                || !Character.isDigit(value.charAt(pos + 5))) {
                throw new NumberFormatException("bad time zone"); //$NON-NLS-1$
            }
            final int hours = (value.charAt(pos + 1) - '0') * 10 + value.charAt(pos + 2) - '0';
            final int mins = (value.charAt(pos + 4) - '0') * 10 + value.charAt(pos + 5) - '0';
            int milliseconds = (hours * 60 + mins) * 60 * 1000;

            // subtract milliseconds from current date to obtain GMT
            if (value.charAt(pos) == '+') {
                milliseconds = -milliseconds;
            }
            date.setTime(date.getTime() + milliseconds);
            pos += 6;
        }
        if (pos < value.length() && value.charAt(pos) == 'Z') {
            pos++;
            calendar.setTimeZone(UTC_TIME_ZONE);
        }
        if (pos < value.length()) {
            throw new NumberFormatException("bad characters"); //$NON-NLS-1$
        }
        calendar.setTime(date);

        // support dates before the Christian era
        if (bc) {
            calendar.set(Calendar.ERA, GregorianCalendar.BC);
        }

        return calendar;
    }

    /**
     * <p>
     * Time zones in datetime strings:
     * </p>
     * <ul>
     * <li>If the string specifies the UTC time zone with the literal "Z", the
     * string refers to a UTC time and the returned {@link Calendar} is set to
     * use the UTC time zone.</li>
     * <li>If the string does not specify a time zone, the string refers to a
     * UTC time but the returned {@link Calendar} is set to use the
     * <i>default</i> time zone.</li>
     * <li>If any other time zone is specified, the string refers to a time in
     * the specified time zone, but the returned {@link Calendar} is set to use
     * the <i>default</i> time zone.
     * </ul>
     * <p>
     *
     * <pre>
     * Copyright 2001-2004 The Apache Software Foundation.
     * Licensed under the Apache License, Version 2.0 (the &quot;License&quot;); you may
     * not use this file except in compliance with the License. You may obtain a
     * copy of the License at
     * http://www.apache.org/licenses/LICENSE-2.0
     * Unless required by applicable law or agreed to in writing, software
     * distributed under the License is distributed on an &quot;AS IS&quot; BASIS, WITHOUT
     * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
     * License for the specific language governing permissions and limitations
     * under the License.
     * </pre>
     *
     * </p>
     *
     * @author Sam Ruby (rubys@us.ibm.com)
     * @author Rich Scheuerle (scheu@us.ibm.com) Modified for JAX-RPC
     */
    public static Calendar parseDateTime(String value) {
        final Calendar calendar = Calendar.getInstance();
        Date date;
        boolean bc = false;

        // validate fixed portion of format
        if (value == null || value.length() == 0) {
            throw new NumberFormatException("Bad datetime value"); //$NON-NLS-1$
        }
        if (value.charAt(0) == '+') {
            value = value.substring(1);
        }
        if (value.charAt(0) == '-') {
            value = value.substring(1);
            bc = true;
        }
        if (value.length() < 19) {
            throw new NumberFormatException("Bad datetime value"); //$NON-NLS-1$
        }
        if (value.charAt(4) != '-' || value.charAt(7) != '-' || value.charAt(10) != 'T') {
            throw new NumberFormatException("bad date"); //$NON-NLS-1$
        }
        if (value.charAt(13) != ':' || value.charAt(16) != ':') {
            throw new NumberFormatException("bad time"); //$NON-NLS-1$
        }
        // convert what we have validated so far
        try {
            synchronized (UTC_DATETIME_FORMAT) {
                date = UTC_DATETIME_FORMAT.parse(value.substring(0, 19) + ".000Z"); //$NON-NLS-1$
            }
        } catch (final Exception e) {
            throw new NumberFormatException(e.toString());
        }
        int pos = 19;

        // parse optional milliseconds
        if (pos < value.length() && value.charAt(pos) == '.') {
            int milliseconds = 0;
            final int start = ++pos;
            while (pos < value.length() && Character.isDigit(value.charAt(pos))) {
                pos++;
            }
            final String decimal = value.substring(start, pos);
            if (decimal.length() == 3) {
                milliseconds = Integer.parseInt(decimal);
            } else if (decimal.length() < 3) {
                milliseconds = Integer.parseInt((decimal + "000").substring(0, 3)); //$NON-NLS-1$
            } else {
                milliseconds = Integer.parseInt(decimal.substring(0, 3));
                if (decimal.charAt(3) >= '5') {
                    ++milliseconds;
                }
            }

            // add milliseconds to the current date
            date.setTime(date.getTime() + milliseconds);
        }

        // parse optional timezone
        if (pos + 5 < value.length() && (value.charAt(pos) == '+' || (value.charAt(pos) == '-'))) {
            if (!Character.isDigit(value.charAt(pos + 1))
                || !Character.isDigit(value.charAt(pos + 2))
                || value.charAt(pos + 3) != ':'
                || !Character.isDigit(value.charAt(pos + 4))
                || !Character.isDigit(value.charAt(pos + 5))) {
                throw new NumberFormatException("bad time zone"); //$NON-NLS-1$
            }
            final int hours = (value.charAt(pos + 1) - '0') * 10 + value.charAt(pos + 2) - '0';
            final int mins = (value.charAt(pos + 4) - '0') * 10 + value.charAt(pos + 5) - '0';
            int milliseconds = (hours * 60 + mins) * 60 * 1000;

            // subtract milliseconds from current date to obtain GMT
            if (value.charAt(pos) == '+') {
                milliseconds = -milliseconds;
            }
            date.setTime(date.getTime() + milliseconds);
            pos += 6;
        }
        if (pos < value.length() && value.charAt(pos) == 'Z') {
            pos++;
            calendar.setTimeZone(UTC_TIME_ZONE);
        }
        if (pos < value.length()) {
            throw new NumberFormatException("bad characters"); //$NON-NLS-1$
        }
        calendar.setTime(date);

        // support dates before the Christian era
        if (bc) {
            calendar.set(Calendar.ERA, GregorianCalendar.BC);
        }

        return calendar;
    }
}
