// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.vss.client.core.utils;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.UUID;

import com.microsoft.tfs.core.Messages;

public abstract class ArgumentUtility {

    public final static String EMPTY_UUID_STRING = "00000000-0000-0000-0000-000000000000"; //$NON-NLS-1$
    public final static UUID EMPTY_UUID = UUID.fromString(EMPTY_UUID_STRING);

    /**
     * Check input arguments for null
     *
     * @param var
     * @param varName
     */
    public static void checkForNull(final Object var, final String varName) {
        if (var == null) {
            throw new IllegalArgumentException(
                MessageFormat.format(Messages.getString("ArgumentUtil.NullNotAllowedFormat"), varName)); //$NON-NLS-1$
        }
    }

    /**
     * Check input string for null or empty
     *
     * @param stringVar
     * @param stringVarName
     */
    public static void checkStringForNullOrEmpty(final String stringVar, final String stringVarName) {
        checkStringForNullOrEmpty(stringVar, stringVarName, false);
    }

    /**
     * Check input string for null or empty
     *
     * @param stringVar
     * @param stringVarName
     * @param trim
     */
    public static void checkStringForNullOrEmpty(String stringVar, final String stringVarName, final boolean trim) {
        checkForNull(stringVar, stringVarName);
        if (trim == true) {
            stringVar = stringVar.trim();
        }
        if (stringVar.length() == 0) {
            throw new IllegalArgumentException(
                MessageFormat.format(Messages.getString("ArgumentUtil.EmptyStringNotAllowedFormat"), stringVarName)); //$NON-NLS-1$
        }
    }

    /**
     * check input collection for null or empty
     *
     * @param collection
     * @param collectionName
     */
    public static void checkCollectionForNullOrEmpty(final Collection<?> collection, final String collectionName) {
        checkForNull(collection, collectionName);
        if (collection.size() == 0) {
            throw new IllegalArgumentException(
                MessageFormat.format(
                    Messages.getString("ArgumentUtil.EmptyCollectionNotAllowedFormat"), //$NON-NLS-1$
                    collectionName));
        }
    }

    /**
     * check input guid for empty
     *
     * @param guid
     * @param varName
     */
    public static void checkForEmptyGuid(final UUID guid, final String varName) {
        checkForNull(guid, varName);
        if (guid.equals(EMPTY_UUID)) {
            throw new IllegalArgumentException(
                MessageFormat.format(
                    Messages.getString("ArgumentUtil.EmptyGuidNotAllowedFormat"), //$NON-NLS-1$
                    varName,
                    EMPTY_UUID_STRING));
        }
    }
}
