// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.util;

/**
 *         Static utility methods for checking method inputs for valid states
 *         (not null, not empty, non zero, etc.). Unlike (language keyword)
 *         assert usage, these checks remain in production binaries.
 */
public abstract class Check {
    /**
     * Throws NullPointerException if the given object is null.
     *
     * @param o
     *        the object to check.
     * @param variableName
     *        the name of the variable being checked (may be null).
     */
    public static void notNull(final Object o, final String variableName) {
        if (o == null) {
            throwForNull(variableName);
        }
    }

    /**
     * Throws IllegalArgumentException if the given string is not null and its
     * length is 0.
     *
     * @param string
     *        the string to check.
     * @param variableName
     *        the name of the variable being checked (may be null).
     */
    public static void notEmpty(final String string, final String variableName) {
        if (string != null && string.length() == 0) {
            throwForEmpty(variableName);
        }
    }

    /**
     * Throws NullPointerException if the given string is null,
     * IllegalArgumentException if the given string's length is 0.
     *
     * @param string
     *        the string to check.
     * @param variableName
     *        the name of the variable being checked (may be null).
     */
    public static void notNullOrEmpty(final String string, final String variableName) {
        if (string == null) {
            throwForNull(variableName);
        }
        if (string.length() == 0) {
            throwForEmpty(variableName);
        }
    }

    /**
     * Throws NullPointerException if the given array is null,
     * IllegalArgumentException if the given array's length is 0.
     *
     * @param array
     *        the array to check.
     * @param variableName
     *        the name of the variable being checked (may be null).
     */
    public static void notNullOrEmpty(final Object[] array, final String variableName) {
        if (array == null) {
            throwForNull(variableName);
        }
        if (array.length == 0) {
            throwForEmpty(variableName);
        }
    }

    /**
     * Throws IllegalArgumentException if the given condition is false.
     *
     * @param condition
     *        the condition to test.
     */
    public static void isTrue(final boolean condition) {
        isTrue(condition, null);
    }

    /**
     * Throws IllegalArgumentException if the given condition is false.
     *
     * @param condition
     *        the condition to test.
     * @param message
     *        the message to put in the exception (may be null for a generic
     *        message).
     */
    public static void isTrue(final boolean condition, final String message) {
        if (condition == false) {
            throwForFalse(message);
        }
    }

    /**
     * Throws a NullPointerException formatted with a string for the given
     * variable name.
     *
     * @param variableName
     *        the name of the variable being checked (may be null for a generic
     *        message).
     */
    private static void throwForNull(String variableName) {
        // This is the best we can do.
        if (variableName == null) {
            variableName = "argument"; //$NON-NLS-1$
        }

        throw new NullPointerException(variableName + " must not be null"); //$NON-NLS-1$
    }

    /**
     * Throws an IllegalArgumentException formatted with a string for the given
     * variable name.
     *
     * @param variableName
     *        the name of the variable being checked (may be null for a generic
     *        message).
     */
    private static void throwForEmpty(String variableName) {
        // This is the best we can do.
        if (variableName == null) {
            variableName = "argument"; //$NON-NLS-1$
        }

        throw new IllegalArgumentException(variableName + " must not be empty"); //$NON-NLS-1$
    }

    /**
     * Throws an IllegalArgumentException formatted with a message for when a
     * boolean condition is false.
     *
     * @param message
     *        the message to be included in the exception (may be null for a
     *        generic message).
     */
    private static void throwForFalse(String message) {
        // This is the best we can do.
        if (message == null) {
            message = "condition must not be false"; //$NON-NLS-1$
        }

        throw new IllegalArgumentException(message);
    }
}
