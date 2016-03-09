// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.webservices;

import java.text.MessageFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.core.clients.versioncontrol.PropertyUtils;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.datetime.DotNETDate;

public class PropertyValidation {
    // Limits on the sizes of property values
    public static final int MAX_PROPERTY_NAME_LENGTH_IN_CHARS = 400;
    public static final int MAX_BYTE_VALUE_SIZE = 8 * 1024 * 1024;
    public static final int MAX_STRING_VALUE_LENGTH = 4 * 1024 * 1024;

    // Minium date time allowed for a property value.
    public static final Calendar MIN_ALLOWED_DATE_TIME;

    // Maximum date time allowed for a property value.
    public static final Calendar MAX_ALLOWED_DATE_TIME;

    static {
        MIN_ALLOWED_DATE_TIME = new GregorianCalendar(TimeZone.getTimeZone("UTC")); //$NON-NLS-1$
        MIN_ALLOWED_DATE_TIME.set(1753, Calendar.JANUARY, 1, 0, 0, 0);

        // We can't preserve DateTime.MaxValue faithfully because SQL's cut-off
        // is 3 milliseconds lower. Also to handle UTC to Local shifts, we give
        // ourselves a buffer of one day.
        MAX_ALLOWED_DATE_TIME = new GregorianCalendar(TimeZone.getTimeZone("UTC")); //$NON-NLS-1$
        MAX_ALLOWED_DATE_TIME.setTimeInMillis(DotNETDate.MAX_CALENDAR.getTimeInMillis());
        MAX_ALLOWED_DATE_TIME.add(Calendar.DAY_OF_MONTH, -1);
    }

    public static double MIN_NEGATIVE = Double.parseDouble("-1.79E+308"); //$NON-NLS-1$
    public static double MAX_NEGATIVE = Double.parseDouble("-2.23E-308"); //$NON-NLS-1$
    public static double MIN_POSITIVE = Double.parseDouble("2.23E-308"); //$NON-NLS-1$
    public static double MAX_POSITIVE = Double.parseDouble("1.79E+308"); //$NON-NLS-1$

    /**
     * Make sure the property name conforms to the requirements for a property
     * name.
     *
     * @throws TeamFoundationPropertyValidationException
     *         if the name is invalid
     */
    public static void validatePropertyName(final String propertyName)
        throws TeamFoundationPropertyValidationException {
        validatePropertyString(propertyName, MAX_PROPERTY_NAME_LENGTH_IN_CHARS, "propertyName"); //$NON-NLS-1$

        /*
         * Key must not start or end in whitespace. ValidatePropertyString()
         * checks for null and empty strings, which is why indexing on length
         * without re-checking String.IsNullOrEmpty() is ok.
         */
        if (Character.isWhitespace(propertyName.charAt(0))
            || Character.isWhitespace(propertyName.charAt(propertyName.length() - 1))) {
            throw new TeamFoundationPropertyValidationException(
                propertyName,
                MessageFormat.format(
                    Messages.getString("PropertyValidation.InvalidPropertyNameFormat"), //$NON-NLS-1$
                    propertyName));
        }
    }

    /**
     * Make sure the property value is within the supported range of values for
     * the type of the property specified.
     *
     * @throws TeamFoundationPropertyValidationException
     *         if the value is invalid
     */
    public static void validatePropertyValue(final String propertyName, final Object value)
        throws TeamFoundationPropertyValidationException {
        // Keep this consistent with XmlPropertyWriter.Write.
        if (null != value) {
            if (value instanceof byte[]) {
                validateByteArray(propertyName, (byte[]) value);
            } else if (value.getClass().isArray()) {
                throw new PropertyTypeNotSupportedException(propertyName, value.getClass());
            } else if (value instanceof Integer) {
                validateInteger(propertyName, (Integer) value);
            } else if (value instanceof Double) {
                validateDouble(propertyName, (Double) value);
            } else if (value instanceof Calendar) {
                validateCalendar(propertyName, (Calendar) value);
            } else if (value instanceof String) {
                validateStringValue(propertyName, (String) value);
            } else {
                // Here are the remaining types. All are supported over in
                // DbArtifactPropertyValueColumns.
                // With a property definition they'll be strongly-typed when
                // they're read back.
                // Otherwise they read back as strings.
                // Boolean
                // Char
                // SByte
                // Byte
                // Int16
                // UInt16
                // UInt32
                // Int64
                // UInt64
                // Single
                // Decimal
                validateStringValue(propertyName, value.toString());
            }
        }
    }

    private static void validateStringValue(final String propertyName, final String propertyValue) {
        if (propertyValue.length() > MAX_STRING_VALUE_LENGTH) {
            throw new TeamFoundationPropertyValidationException(
                "value", //$NON-NLS-1$
                MessageFormat.format(
                    Messages.getString("PropertyValidation.InvalidPropertyValueSizeFormat"), //$NON-NLS-1$
                    propertyName,
                    String.class.getName(),
                    MAX_STRING_VALUE_LENGTH));

        }
        PropertyUtils.checkForInvalidCharacters(propertyValue, "value"); //$NON-NLS-1$
    }

    private static void validateByteArray(final String propertyName, final byte[] propertyValue) {
        if (propertyValue.length > MAX_BYTE_VALUE_SIZE) {
            throw new TeamFoundationPropertyValidationException(
                "value", //$NON-NLS-1$
                MessageFormat.format(
                    Messages.getString("PropertyValidation.InvalidPropertyValueSizeFormat"), //$NON-NLS-1$
                    propertyName,
                    propertyValue.getClass().getName(),
                    MAX_BYTE_VALUE_SIZE));
        }
    }

    private static void validateCalendar(final String propertyName, final Calendar propertyValue) {
        final Calendar copy = (Calendar) propertyValue.clone();

        // Let users get an out of range error for MinValue and MaxValue, not a
        // DateTimeKind unspecified error.
        if (!copy.equals(DotNETDate.MIN_CALENDAR) && !copy.equals(DotNETDate.MAX_CALENDAR)) {
            // Make sure the property value is in Universal time.
            if (copy.getTime().getTimezoneOffset() != 0) {
                copy.setTimeZone(TimeZone.getTimeZone("UTC")); //$NON-NLS-1$
            }
        }

        checkRange(copy, MIN_ALLOWED_DATE_TIME, MAX_ALLOWED_DATE_TIME, propertyName, "value"); //$NON-NLS-1$
    }

    private static void validateDouble(final String propertyName, final Double propertyValue) {
        if (Double.isInfinite(propertyValue) || Double.isNaN(propertyValue)) {
            throw new TeamFoundationPropertyValidationException(
                "value", //$NON-NLS-1$
                MessageFormat.format(
                    Messages.getString("PropertyValidation.DoubleValueOutOfRangeFormat"), //$NON-NLS-1$
                    propertyName,
                    propertyValue));
        }

        // SQL Server support: - 1.79E+308 to -2.23E-308, 0 and 2.23E-308 to
        // 1.79E+308
        if (propertyValue < MIN_NEGATIVE
            || (propertyValue < 0 && propertyValue > MAX_NEGATIVE)
            || propertyValue > MAX_POSITIVE
            || (propertyValue > 0 && propertyValue < MIN_POSITIVE)) {
            throw new TeamFoundationPropertyValidationException(
                "value", //$NON-NLS-1$
                MessageFormat.format(
                    Messages.getString("PropertyValidation.DoubleValueOutOfRangeFormat"), //$NON-NLS-1$
                    propertyName,
                    propertyValue));
        }
    }

    private static void validateInteger(final String propertyName, final int propertyValue) {
        // All values allowed.
    }

    /**
     * Validation helper for validating all property strings.
     */
    private static void validatePropertyString(
        final String propertyString,
        final int maxSize,
        final String argumentName) {
        Check.notNullOrEmpty(propertyString, argumentName);
        if (propertyString.length() > maxSize) {
            throw new TeamFoundationPropertyValidationException(
                argumentName,
                MessageFormat.format(
                    Messages.getString("PropertyValidation.PropertyArgumentExceededMaximumSizeAllowedFormat"), //$NON-NLS-1$
                    argumentName,
                    maxSize));
        }
        PropertyUtils.checkForInvalidCharacters(propertyString, argumentName);
    }

    public static void checkPropertyLength(
        String propertyValue,
        final Boolean allowNull,
        final int minLength,
        final int maxLength,
        final String propertyName,
        final Class<? extends Object> containerType,
        final String topLevelParamName) {
        boolean valueIsInvalid = false;

        if (propertyValue == null) {
            if (!allowNull) {
                valueIsInvalid = true;
            }
        } else if ((propertyValue.length() < minLength) || (propertyValue.length() > maxLength)) {
            valueIsInvalid = true;
        }

        // throw exception if the value is invalid.
        if (valueIsInvalid) {
            // If the propertyValue is null, just print it like an empty string.
            if (propertyValue == null) {
                propertyValue = ""; //$NON-NLS-1$
            }

            if (allowNull) {
                // paramName comes second for ArgumentException.
                throw new IllegalArgumentException(
                    MessageFormat.format(
                        Messages.getString("PropertyValidation.InvalidStringPropertyValueNullAllowedFormat"), //$NON-NLS-1$
                        propertyValue,
                        propertyName,
                        containerType.getName(),
                        minLength,
                        maxLength));
            } else {
                throw new IllegalArgumentException(
                    MessageFormat.format(
                        Messages.getString("PropertyValidation.InvalidStringPropertyValueNullForbiddenFormat"), //$NON-NLS-1$
                        propertyValue,
                        propertyName,
                        containerType.getClass().getName(),
                        minLength,
                        maxLength));
            }
        }
    }

    /**
     * Verify that a propery is within the bounds of the specified range.
     */
    public static <T extends Comparable<T>> void checkRange(
        final T propertyValue,
        final T minValue,
        final T maxValue,
        final String propertyName,
        final Class<? extends Object> containerType,
        final String topLevelParamName) {
        if (propertyValue.compareTo(minValue) < 0 || propertyValue.compareTo(maxValue) > 0) {
            throw new IllegalArgumentException(
                MessageFormat.format(
                    Messages.getString("PropertyValidation.ValueTypeOutOfRangeFormat"), //$NON-NLS-1$
                    propertyValue,
                    propertyName,
                    containerType.getClass().getName(),
                    minValue,
                    maxValue));
        }
    }

    private static <T extends Comparable<T>> void checkRange(
        final T propertyValue,
        final T minValue,
        final T maxValue,
        final String propertyName,
        final String topLevelParamName) {
        if (propertyValue.compareTo(minValue) < 0 || propertyValue.compareTo(maxValue) > 0) {
            // paramName comes first for ArgumentOutOfRangeException.
            throw new IllegalArgumentException(
                MessageFormat.format(
                    Messages.getString("PropertyValidation.PropertyValueOutOfRangeFormat"), //$NON-NLS-1$
                    propertyValue,
                    propertyName,
                    minValue,
                    maxValue));
        }
    }

    /**
     * Make sure the property filter conforms to the requirements for a property
     * filter.
     */
    public static void validatePropertyFilter(final String propertyNameFilter) {
        PropertyValidation.validatePropertyString(
            propertyNameFilter,
            MAX_PROPERTY_NAME_LENGTH_IN_CHARS,
            "propertyNameFilter"); //$NON-NLS-1$
    }
}