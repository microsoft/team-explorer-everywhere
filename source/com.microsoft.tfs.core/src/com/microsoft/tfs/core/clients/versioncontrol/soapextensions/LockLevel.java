// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.soapextensions;

import java.text.MessageFormat;
import java.util.HashMap;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.core.internal.wrappers.EnumerationWrapper;
import com.microsoft.tfs.util.Check;

import ms.tfs.versioncontrol.clientservices._03._LockLevel;

/**
 * Enumerates the types levels available for version control items.
 *
 * @since TEE-SDK-10.1
 */
public class LockLevel extends EnumerationWrapper {
    /**
     * A map of enumeration integer values to {@link LockLevel}
     */
    public static final HashMap<Byte, LockLevel> VALUE_MAP = new HashMap<Byte, LockLevel>();

    public static final LockLevel NONE = new LockLevel(_LockLevel.None, (byte) 0);
    public static final LockLevel CHECKIN = new LockLevel(_LockLevel.Checkin, (byte) 1);
    public static final LockLevel CHECKOUT = new LockLevel(_LockLevel.CheckOut, (byte) 2);
    public static final LockLevel UNCHANGED = new LockLevel(_LockLevel.Unchanged, (byte) 3);

    /**
     * The enumeration numeric value for this enumeration.
     */
    private final byte value;

    private LockLevel(final _LockLevel lockLevel, final byte value) {
        super(lockLevel);
        this.value = value;

        final Byte key = new Byte(value);
        Check.isTrue(!VALUE_MAP.containsKey(key), "duplicate key"); //$NON-NLS-1$
        VALUE_MAP.put(key, this);
    }

    /**
     * Gets the {@link LockLevel} associated with the specified integer value.
     *
     * @param value
     *        The integer value for this item type.
     * @return The {@link LockLevel} with the associated integer value.
     */
    public static LockLevel fromByteValue(final byte value) {
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
    public static LockLevel fromWebServiceObject(final _LockLevel webServiceObject) {
        return (LockLevel) EnumerationWrapper.fromWebServiceObject(webServiceObject);
    }

    /**
     * Gets the web service object this class wraps. The returned object should
     * not be modified.
     *
     * @return the web service object this class wraps.
     */
    public _LockLevel getWebServiceObject() {
        return (_LockLevel) webServiceObject;
    }

    /**
     * Returns the integer value associated with this enumeration.
     */
    public byte getValue() {
        return value;
    }

    /**
     * The localized string appropriate for normal text lock level display
     * (command-line client "status /format:detailed" command, for example).
     *
     * @return the short string that describes the lock level. If the lock level
     *         is null, "none" is returned.
     */
    public String toUIString() {
        if (this == LockLevel.NONE) {
            return Messages.getString("LockLevel.None"); //$NON-NLS-1$
        } else if (this == LockLevel.CHECKIN) {
            return Messages.getString("LockLevel.Checkin"); //$NON-NLS-1$
        } else if (this == LockLevel.CHECKOUT) {
            return Messages.getString("LockLevel.CheckOut"); //$NON-NLS-1$
        } else if (this == LockLevel.UNCHANGED) {
            return Messages.getString("LockLevel.Unchanged"); //$NON-NLS-1$
        }

        throw new RuntimeException(MessageFormat.format("Unknown LockLevel {0}", toString())); //$NON-NLS-1$
    }

    /**
     * The localized string appropriate for very space-constrained lock level
     * display (command-line client "status /format:brief" command, for
     * example). The strings returned are one character long.
     *
     * @return the (very) short string that describes the lock level.
     */
    public String toShortUIString() {
        if (this == LockLevel.NONE) {
            return " "; //$NON-NLS-1$
        } else if (this == LockLevel.CHECKIN) {
            return Messages.getString("LockLevel.LockCheckinShort"); //$NON-NLS-1$
        } else if (this == LockLevel.CHECKOUT) {
            return Messages.getString("LockLevel.LockCheckOutShort"); //$NON-NLS-1$
        } else if (this == LockLevel.UNCHANGED) {
            return " "; //$NON-NLS-1$
        }

        throw new RuntimeException(MessageFormat.format("Unknown LockLevel {0}", toString())); //$NON-NLS-1$
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
