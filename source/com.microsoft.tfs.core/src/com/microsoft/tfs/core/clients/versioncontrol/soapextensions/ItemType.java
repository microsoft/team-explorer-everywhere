// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.soapextensions;

import java.util.HashMap;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.core.internal.wrappers.EnumerationWrapper;
import com.microsoft.tfs.util.Check;

import ms.tfs.versioncontrol.clientservices._03._ItemType;

/**
 * Enumerates the types of version control objects.
 *
 * @since TEE-SDK-10.1
 */
public class ItemType extends EnumerationWrapper {
    /**
     * A map of enumeration integer values to ItemType
     */
    public static final HashMap<Byte, ItemType> VALUE_MAP = new HashMap<Byte, ItemType>();

    /**
     * Matches any kind of item (file, folder, other).
     */
    public static final ItemType ANY = new ItemType(_ItemType.Any, (byte) 0);

    /**
     * Matches only folder objects (which may contain other folders and files).
     */
    public static final ItemType FOLDER = new ItemType(_ItemType.Folder, (byte) 1);

    /**
     * Matches only file objects (which may not contain files or folders).
     */
    public static final ItemType FILE = new ItemType(_ItemType.File, (byte) 2);

    /**
     * The enumeration numeric value for this enumeration.
     */
    private final byte value;

    private ItemType(final _ItemType itemType, final byte value) {
        super(itemType);
        this.value = value;

        final Byte key = new Byte(value);
        Check.isTrue(!VALUE_MAP.containsKey(key), "duplicate key"); //$NON-NLS-1$
        VALUE_MAP.put(key, this);
    }

    /**
     * Gets the ItemType associated with the specified integer value.
     *
     * @param value
     *        The integer value for this item type.
     * @return The ItemType with the associated integer value.
     */
    public static ItemType fromByteValue(final byte value) {
        final Byte key = new Byte(value);
        Check.isTrue(VALUE_MAP.containsKey(key), "value"); //$NON-NLS-1$
        return VALUE_MAP.get(key);
    }

    /**
     * Gets the correct wrapper type for the given web service object.
     *
     * @param webServiceObject
     *        the web service object (must not be <code>null</code>)
     * @return the correct wrapper type for the given web service object
     * @throws RuntimeException
     *         if no wrapper type is known for the given web service object
     */
    public static ItemType fromWebServiceObject(final _ItemType webServiceObject) {
        return (ItemType) EnumerationWrapper.fromWebServiceObject(webServiceObject);
    }

    /**
     * Gets the web service object this class wraps. The returned object should
     * not be modified.
     *
     * @return the web service object this class wraps.
     */
    public _ItemType getWebServiceObject() {
        return (_ItemType) webServiceObject;
    }

    /**
     * Returns the integer value associated with this enumeration.
     */
    public byte getValue() {
        return value;
    }

    /**
     * The localized string appropriate for normal item type display.
     *
     * @return the localized string that describes the given item type.
     */
    public String toUIString() {
        if (this == FILE) {
            return Messages.getString("ItemType.File"); //$NON-NLS-1$
        } else if (this == FOLDER) {
            return Messages.getString("ItemType.Folder"); //$NON-NLS-1$
        }

        // Visual Studio returns empty string for non-file, non-folder
        return ""; //$NON-NLS-1$
    }

    @Override
    public boolean equals(final Object o) {
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
