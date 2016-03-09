// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.soapextensions;

import java.util.HashMap;

import com.microsoft.tfs.core.internal.wrappers.EnumerationWrapper;
import com.microsoft.tfs.util.Check;

import ms.tfs.versioncontrol.clientservices._03._ConflictType;

/**
 * Describes the conflict type.
 *
 * @since TEE-SDK-10.1
 */
public class ConflictType extends EnumerationWrapper {
    public static final HashMap<Integer, ConflictType> VALUE_MAP = new HashMap<Integer, ConflictType>();

    public static final ConflictType NONE = new ConflictType(_ConflictType.None, 0);
    public static final ConflictType GET = new ConflictType(_ConflictType.Get, 1);
    public static final ConflictType CHECKIN = new ConflictType(_ConflictType.Checkin, 2);
    public static final ConflictType LOCAL = new ConflictType(_ConflictType.Local, 3);
    public static final ConflictType MERGE = new ConflictType(_ConflictType.Merge, 4);
    public static final ConflictType UNKNOWN = new ConflictType(_ConflictType.Unknown, 5);

    private final int value;

    private ConflictType(final _ConflictType conflictType, final int value) {
        super(conflictType);
        this.value = value;

        final Integer key = new Integer(value);
        Check.isTrue(!VALUE_MAP.containsKey(key), "duplicate key"); //$NON-NLS-1$
        VALUE_MAP.put(key, this);
    }

    /**
     * Returns the ConflictType for the specified integer value.
     *
     * @param value
     *        The integer value.
     * @return The ConflictType corresponding to this value.
     */
    public static ConflictType fromInteger(final int value) {
        final Integer key = new Integer(value);
        Check.isTrue(VALUE_MAP.containsKey(key), "VALUE_MAP.containsKey(key)"); //$NON-NLS-1$
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
    public static ConflictType fromWebServiceObject(final _ConflictType webServiceObject) {
        return (ConflictType) EnumerationWrapper.fromWebServiceObject(webServiceObject);
    }

    /**
     * Gets the web service object this class wraps. The returned object should
     * not be modified.
     *
     * @return the web service object this class wraps.
     */
    public _ConflictType getWebServiceObject() {
        return (_ConflictType) webServiceObject;
    }

    public int getValue() {
        return value;
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
