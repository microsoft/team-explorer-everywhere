// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.core.clients.versioncontrol.internal.localworkspace.WorkspaceLocalItem;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.LocalPendingChange;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PropertyValue;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.FileHelpers;

/**
 * Static methods to validate TFS 2010/2012 property names and values.
 *
 * @threadsafety thread-safe
 */
public abstract class PropertyUtils {
    /**
     * The maximum length in characters of a property name string.
     */
    private static final int MAXIMUM_PROPERTY_NAME_SIZE = 800;

    /**
     * Checks each of the given property name filter strings for length and
     * content compliance.
     *
     * @param propertyNameFilters
     *        the property name filters to check (must not be <code>null</code>)
     */
    public static void validatePropertyFilters(final String[] propertyNameFilters) {
        PropertyUtils.validatePropertyStrings(
            propertyNameFilters,
            PropertyUtils.MAXIMUM_PROPERTY_NAME_SIZE,
            "propertyNameFilters"); //$NON-NLS-1$
    }

    /**
     * Checks a single property name filter for length and content compliance.
     *
     * @param propertyNameFilter
     *        the property name filter to check (must not be <code>null</code>)
     */
    public static void validatePropertyFilter(final String propertyNameFilter) {
        PropertyUtils.validatePropertyString(propertyNameFilter, MAXIMUM_PROPERTY_NAME_SIZE, "propertyNameFilter"); //$NON-NLS-1$
    }

    /**
     * Checks each of the given strings for size and content compliance via
     * {@link #validatePropertyString(String, int, String)}.
     *
     * @param strings
     *        the strings to check (must not be <code>null</code>)
     * @param maxSize
     *        the maximum size of each string (must not be <code>null</code>)
     * @param argumentName
     *        the name of the array of strings to use in error text (must not be
     *        <code>null</code>)
     */
    private static void validatePropertyStrings(final String[] strings, final int maxSize, final String argumentName) {
        Check.notNull(strings, argumentName);
        for (int i = 0; i < strings.length; i++) {
            /*
             * This method checks for a null array member.
             */
            PropertyUtils.validatePropertyString(strings[i], maxSize, argumentName + "[" + i + "]"); //$NON-NLS-1$ //$NON-NLS-2$
        }
    }

    /**
     * Checks the given property string for size and content compliance,
     * throwing {@link IllegalArgumentException} if the string is invalid.
     *
     * @param string
     *        the string to check (must not be <code>null</code>)
     * @param maxSize
     *        the maximum size of the string (must not be <code>null</code>)
     * @param argumentName
     *        the name of the string to use in error text (must not be
     *        <code>null</code>)
     */
    private static void validatePropertyString(final String string, final int maxSize, final String argumentName) {
        Check.notNullOrEmpty(string, argumentName);

        if (string.length() > maxSize) {
            throw new IllegalArgumentException(
                MessageFormat.format(
                    Messages.getString("PropertyUtils.PropertyNameStoredAtLocationExceedsMaximumSizeCharactersFormat"), //$NON-NLS-1$
                    string,
                    argumentName,
                    maxSize));
        }

        PropertyUtils.checkForInvalidCharacters(string, argumentName);
    }

    /**
     * Checks the given string for invalid (control) characters, and throws
     * {@link IllegalArgumentException} if found.
     *
     * @param string
     *        the string to check (must not be <code>null</code>)
     * @param argumentName
     *        the name of the string to use in error text (must not be
     *        <code>null</code>)
     * @throws IllegalArgumentException
     *         if the string had invalid characters
     */
    public static void checkForInvalidCharacters(final String string, final String argumentName)
        throws IllegalArgumentException {
        Check.notNull(string, argumentName);

        for (int i = 0; i < string.length(); i++) {
            /*
             * TODO Visual Studio uses .NET's Character.IsControl(), which might
             * be more picky (it appears to find Unicode control characters that
             * are non-ISO).
             */
            if (Character.isISOControl(string.charAt(i))) {
                throw new IllegalArgumentException(
                    MessageFormat.format(
                        Messages.getString("PropertyUtils.UnicodeCharacterValueInArgumentIsControlFormat"), //$NON-NLS-1$
                        (int) string.charAt(i),
                        argumentName));
            }
        }
    }

    /**
     * Selects the "dirty" {@link PropertyValue}s from the given array and
     * returns only those.
     *
     * @param propertyValues
     *        the property values to select dirty ones from (must not be
     *        <code>null</code>)
     * @return the array of "dirty" property values
     */
    public static PropertyValue[] selectDirtyPropertyValues(final PropertyValue[] propertyValues) {
        Check.notNull(propertyValues, "propertyValues"); //$NON-NLS-1$

        final List dirtyPropertyValues = new ArrayList();

        for (int i = 0; i < propertyValues.length; i++) {
            Check.notNull(propertyValues[i], "propertyValues[" + i + "]"); //$NON-NLS-1$ //$NON-NLS-2$

            final PropertyValue propertyValue = propertyValues[i];

            if (propertyValue.isDirty()) {
                dirtyPropertyValues.add(propertyValue);
            }
        }

        return (PropertyValue[]) dirtyPropertyValues.toArray(new PropertyValue[dirtyPropertyValues.size()]);
    }

    /**
     * Returns a new array with new {@link PropertyValue} instances matching the
     * given values. The value object inside each {@link PropertyValue} item
     * (byte[], String, Boolean, etc.) is <em>not</em> cloned.
     *
     * @param values
     *        the values to clone (may be <code>null</code> or empty)
     * @return a new array of new values, or <code>null</code> if the given
     *         values array was null
     */
    public static PropertyValue[] clonePropertyValues(final PropertyValue[] values) {
        if (values == null) {
            return null;
        }

        final PropertyValue[] ret = new PropertyValue[values.length];
        for (int i = 0; i < values.length; i++) {
            // Value object not cloned
            ret[i] = new PropertyValue(values[i].getPropertyName(), values[i].getPropertyValue());
        }
        return ret;
    }

    /**
     * Selects the property value that matches the given filter.
     *
     * @param values
     *        the properties to select from (if <code>null</code>,
     *        <code>null</code> is returned)
     * @param itemPropertyFilter
     *        the filter to match (if <code>null</code>, <code>null</code> is
     *        returned)
     * @return the matched {@link PropertyValue} or <code>null</code> if none
     *         matched or none were supplied
     */
    public static PropertyValue selectMatching(final PropertyValue[] values, final String itemPropertyFilter) {
        final PropertyValue[] matches = selectMatching(values, new String[] {
            itemPropertyFilter
        });

        if (matches == null || matches.length == 0) {
            return null;
        }

        return matches[0];
    }

    /**
     * Selects the property values that match the given filters. The result
     * contains only the unique matches (no duplicates).
     *
     * @param values
     *        the properties to select from (if <code>null</code>,
     *        <code>null</code> is returned)
     * @param itemPropertyFilters
     *        the filters to match (if <code>null</code>, <code>null</code> is
     *        returned)
     * @return the matched {@link PropertyValue}s or <code>null</code> if none
     *         matched or none were supplied
     */
    public static PropertyValue[] selectMatching(final PropertyValue[] values, final String[] itemPropertyFilters) {
        if (values == null || values.length == 0 || itemPropertyFilters == null || itemPropertyFilters.length == 0) {
            return null;
        }

        final Set<PropertyValue> matches = new HashSet<PropertyValue>();
        for (final PropertyValue value : values) {
            for (final String filter : itemPropertyFilters) {
                /*
                 * This utility method supports the same wildcards (*, ?)
                 * property filters use. Ignore case.
                 */
                if (filter != null && FileHelpers.filenameMatches(value.getPropertyName(), filter, true)) {
                    matches.add(value);
                    break;
                }
            }
        }

        return matches.toArray(new PropertyValue[matches.size()]);
    }

    /**
     * Returns the unique values from the given values as a new array.
     *
     * @param values
     *        the values to select from (may be <code>null</code> )
     * @return a new array containing the unique values, or <code>null</code> if
     *         the given values were <code>null</code>, or empty if the given
     *         values were emtpy
     * @deprecated remove this method as soon as the TFS bug gets fixed that
     *             returns duplicate property values
     */
    @Deprecated
    public static PropertyValue[] selectUnique(final PropertyValue[] values) {
        if (values == null) {
            return values;
        }

        if (values.length == 0) {
            return new PropertyValue[0];
        }

        // Does the work for us
        return selectMatching(values, PropertyConstants.QUERY_ALL_PROPERTIES_FILTERS);
    }

    /**
     * Merges existing and pending property values.
     *
     * @param existingValues
     *        the existing (typically {@link WorkspaceLocalItem}'s) values (may
     *        be <code>null</code> or empty)
     * @param pendingValues
     *        the pending (typically {@link LocalPendingChange}'s) values (may
     *        be <code>null</code> or empty)
     * @return the merge values, <code>null</code> if both the given value
     *         arrays were <code>null</code> or the merge result is no property
     *         values
     */
    public static PropertyValue[] mergePendingValues(PropertyValue[] existingValues, PropertyValue[] pendingValues) {
        // Convert empty to null for easier testing
        if (existingValues != null && existingValues.length == 0) {
            existingValues = null;
        }
        if (pendingValues != null && pendingValues.length == 0) {
            pendingValues = null;
        }

        if (existingValues != null && pendingValues != null) {
            /*
             * Pending values overwrite the existing values by name
             * (case-insensitive).
             */

            final Map<String, PropertyValue> map = new TreeMap<String, PropertyValue>(String.CASE_INSENSITIVE_ORDER);

            for (final PropertyValue e : existingValues) {
                map.put(e.getPropertyName(), e);
            }

            for (final PropertyValue p : pendingValues) {
                map.put(p.getPropertyName(), p);
            }

            return map.values().toArray(new PropertyValue[map.size()]);
        } else if (existingValues != null) {
            // Pending are null
            return existingValues;
        } else if (pendingValues != null) {
            // Existing are null
            return pendingValues;
        }

        // Both are null
        return null;
    }

    /**
     * Merges two arrays of property filters into one arrays that matches all
     * the items the two arrays would match individually. Returns
     * <code>null</code> when both inputs are <code>null</code> . Order is not
     * preserved.
     *
     * @param filters1
     *        the first array of filters (may be <code>null</code>)
     * @param filters2
     *        the second array of filters (may be <code>null</code>)
     * @return a new array of filters including the filters from both given
     *         arrays of filters, or <code>null</code> if the specified filters
     *         were null and there are no extra filters
     */
    public static String[] mergePropertyFilters(final String[] filters1, final String[] filters2) {
        if (filters1 == null && filters2 == null) {
            return null;
        } else if (filters1 == null) {
            return filters2;
        } else if (filters2 == null) {
            return filters1;
        }
        // Both not null

        if (filters1.length == 0 && filters2.length == 0) {
            return filters1;
        } else if (filters1.length == 0) {
            return filters2;
        } else if (filters2.length == 0) {
            return filters1;
        }
        // Both not null and not empty

        final Set<String> newFilters = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
        newFilters.addAll(Arrays.asList(filters1));
        newFilters.addAll(Arrays.asList(filters2));
        return newFilters.toArray(new String[newFilters.size()]);
    }

    public static boolean equals(PropertyValue[] values1, PropertyValue[] values2) {
        if (values1 == values2) {
            return true;
        }
        if (values1 == null || values2 == null) {
            return false;
        }
        // Both not null

        if (values1.length != values2.length) {
            return false;
        }

        if (values1.length == 0 && values2.length == 0) {
            return true;
        }

        // Avoid the clone and sort if both are length 1
        if (values1.length == 1) {
            return Arrays.equals(values1, values2);
        }

        values1 = values1.clone();
        values2 = values2.clone();

        Arrays.sort(values1);
        Arrays.sort(values2);

        return Arrays.equals(values1, values2);
    }
}
