// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.internal.soapextensions;

import java.util.Calendar;
import java.util.Map;

import com.microsoft.tfs.core.ws.runtime.xml.XMLConvert;
import com.microsoft.tfs.util.StringUtil;
import com.microsoft.tfs.util.datetime.DotNETDate;

public class CommonInformationHelper {
    /**
     * Retreives an integer from the dictionary out of the specified field.
     *
     *
     * @param map
     *        The dictionary which contains the field
     * @param fieldName
     *        The name of the field which contains the integer
     * @return An integer parsed from the string
     */
    public static int GetInt(final Map<String, String> map, final String fieldName) {
        return getInt(map, fieldName, 0);
    }

    /**
     * Retreives an integer from the dictionary out of the specified field.
     *
     *
     * @param map
     *        The dictionary which contains the field
     * @param fieldName
     *        The name of the field which contains the integer
     * @param invalidValue
     *        An integer value that should be returned if the field is not
     *        present or not an integer.
     * @return An integer parsed from the string
     */
    public static int getInt(final Map<String, String> map, final String fieldName, final int invalidValue) {
        final String value = map.get(fieldName);
        if (value == null) {
            return invalidValue;
        } else {
            try {
                return Integer.parseInt(value);
            } catch (final NumberFormatException e) {
                return invalidValue;
            }
        }
    }

    /**
     * Returns a string from the specified field. If the fieldName is missing
     * from the dictionary, String.Empty is returned.
     *
     *
     * @param map
     *        The dictonary which contains the value
     * @param fieldName
     *        The name of the field which contains the value
     * @return The value as a string if found, String.Empty otherwise
     */
    public static String getString(final Map<String, String> map, final String fieldName) {
        final String result = map.get(fieldName);
        if (result != null) {
            return result;
        } else {
            return StringUtil.EMPTY;
        }
    }

    /**
     * Attempts to parse a DateTime object back from the string representation.
     * The string must be formatted using the InvariantInfo provider for
     * DateTime formatting and must be in the RoundTrip format
     * <see>http://msdn2.microsoft.com/en-us/library/az4se3k1.aspx</see>. If
     * these conditions are not met and the DateTime cannot be parsed then
     * DateTime.MinValue is returned.
     *
     *
     * @param map
     *        The dictionary which contains the DateTime to parse
     * @param fieldName
     *        The name at which the DateTime can be found in the dictionary
     * @return A DateTime object translated to the current local time zone
     */
    public static Calendar getDateTime(final Map<String, String> map, final String fieldName) {
        final String value = map.get(fieldName);

        if (value == null) {
            return DotNETDate.MIN_CALENDAR;
        } else {
            return toCalendar(value);
        }
    }

    /**
     * Attempts to convert a string into a DateTime object. The string must be
     * in the round trip format ("o") and should have been created using the
     * InvariantInfo format provider. If the date cannot be parsed using these
     * conditions then the minimum value is returned.
     *
     *
     * @param value
     *        The string value which should be parsed
     * @return A DateTime object which the string represents, if any
     */
    public static Calendar toCalendar(final String value) {
        return XMLConvert.toCalendar(value, true);
    }
}
