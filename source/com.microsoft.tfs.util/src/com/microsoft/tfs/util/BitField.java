// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * {@link BitField} is a base class that can be subclassed to build
 * strongly-typed bitfield-style enumerations. In addition to being
 * strongly-typed, {@link BitField} subclasses are immutable, serializable, and
 * performant (operation cost is equivalent to integer bitfields).
 * </p>
 *
 * <p>
 * Internally, {@link BitField} instances have an immutable integer flag value.
 * All operations on {@link BitField} become bitwise operations on this flag
 * value, which are fast constant-time operations.
 * </p>
 *
 * <p>
 * {@link BitField} subclasses should never be compared using <code>==</code>.
 * Instead, use {@link #equals(Object)}. Two {@link BitField}s are equal if they
 * have the same flag values and are the same {@link BitField} subclass. The
 * hash code is simply the flag value. Unequal {@link BitField}s may have equal
 * hash codes (if they have the same flag value but are different types).
 * Because of this, {@link BitField}s of different types should not be stored in
 * the same hash-based container for best performance.
 * </p>
 */
public abstract class BitField implements Serializable {
    /**
     * The immutable integer flags value for this {@link BitField} instance.
     */
    private final int flags;

    /**
     * Creates a new {@link BitField} instance using the given flags value.
     *
     * @param flags
     *        the integer flags value for this instance
     */
    protected BitField(final int flags) {
        this.flags = flags;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public final boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }

        if (obj == this) {
            return true;
        }

        if (!(obj.getClass() == getClass())) {
            return false;
        }

        return flags == ((BitField) obj).flags;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#hashCode()
     */
    @Override
    public final int hashCode() {
        return flags;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public final String toString() {
        final StringBuffer buffer = new StringBuffer();

        String name = getClass().getName();
        name = name.substring(name.lastIndexOf(".") + 1); //$NON-NLS-1$
        buffer.append(name);

        buffer.append(" (" + flags + "): "); //$NON-NLS-1$ //$NON-NLS-2$

        final String[] stringValues = toStringValues(flags, getClass());
        for (int i = 0; i < stringValues.length; i++) {
            buffer.append(stringValues[i]);
            if (i < stringValues.length - 1) {
                buffer.append(","); //$NON-NLS-1$
            }
        }

        return buffer.toString();
    }

    /**
     * @return <code>true</code> if this {@link BitField} has no bits set
     */
    public final boolean isEmpty() {
        return flags == 0;
    }

    /**
     * @return an array of {@link String} values that represent the flag values
     *         in this {@link BitField}
     */
    public final String[] toStringValues() {
        return toStringValues(flags, getClass());
    }

    /**
     * @return the integer flag values that this {@link BitField} represents
     */
    public final int toIntFlags() {
        return flags;
    }

    /**
     * Called by subclasses to test whether this {@link BitField} contains all
     * of the bits set in the specified {@link BitField}. In set terms, this is
     * the containment (aka inclusion or subset) test.
     *
     * @param other
     *        another {@link BitField} to test (must not be <code>null</code>)
     * @return <code>true</code> if this {@link BitField} contains every bit set
     *         in the specified {@link BitField}
     */
    protected final boolean containsAllInternal(final BitField other) {
        Check.notNull(other, "other"); //$NON-NLS-1$

        return (flags & other.flags) == other.flags;
    }

    /**
     * An alias for the "contains all" test.
     *
     * @see #containsAllInternal(BitField)
     *
     * @param other
     *        another {@link BitField} to test (must not be <code>null</code>)
     * @return <code>true</code> if this {@link BitField} contains every bit set
     *         in the specified {@link BitField}
     */
    protected final boolean containsInternal(final BitField other) {
        return containsAllInternal(other);
    }

    /**
     * Called by subclasses to test whether this {@link BitField} contains any
     * of the bits set in the specified {@link BitField}. In set terms, this is
     * the commonality test.
     *
     * @param other
     *        another {@link BitField} to test (must not be <code>null</code>)
     * @return <code>true</code> if this {@link BitField} contains any bit set
     *         in the specified {@link BitField}
     */
    protected final boolean containsAnyInternal(final BitField other) {
        Check.notNull(other, "other"); //$NON-NLS-1$

        return (flags & other.flags) != 0;
    }

    /**
     * Called by subclasses to compute an integer flag value that represents all
     * the bits set in this {@link BitField}, excluding those bits that are set
     * in the specified {@link BitField}. In set terms, this is the difference
     * (aka relative complement) operation.
     *
     * @param other
     *        another {@link BitField} to difference from this one (must not be
     *        <code>null</code>)
     * @return an integer flag value representing the difference
     */
    protected final int removeInternal(final BitField other) {
        Check.notNull(other, "other"); //$NON-NLS-1$

        return flags & ~other.flags;
    }

    /**
     * Called by subclasses to compute an integer flag value that represents all
     * of the bits that are set in both this {@link BitField} and the specified
     * {@link BitField}. In set terms, this is the intersection operation.
     *
     * @param other
     *        another {@link BitField} to intersect with this one (must not be
     *        <code>null</code>)
     * @return an integer flag value representing the intersection
     */
    protected final int retainInternal(final BitField other) {
        Check.notNull(other, "other"); //$NON-NLS-1$

        return flags & other.flags;
    }

    /**
     * Called by subclasses to compute an integer flag value that represents all
     * of the bits that are set in either this {@link BitField} or the specified
     * {@link BitField}. In set terms, this is the union operation.
     *
     * @param other
     *        another {@link BitField} to union with this one (must not be
     *        <code>null</code>)
     * @return an integer flag value representing the union
     */
    protected final int combineInternal(final BitField other) {
        Check.notNull(other, "other"); //$NON-NLS-1$

        return flags | other.flags;
    }

    /**
     * Internal method used by BitWise enums that wrap flagsets and use
     * "special values". An example is QueryOptions in the build API that has
     * some flags that represent multiple bits.
     *
     * @return
     */
    protected String[] toFullStringValues() {
        BitFieldStringData classData;

        synchronized (PER_CLASS_DATA) {
            classData = (BitFieldStringData) PER_CLASS_DATA.get(getClass());
        }

        if (classData == null) {
            return null;
        }

        IntFlagStringValuePair[] specialValues;
        synchronized (classData.specialValues) {
            specialValues = (IntFlagStringValuePair[]) classData.specialValues.toArray(
                new IntFlagStringValuePair[classData.specialValues.size()]);
        }

        IntFlagStringValuePair[] normalValues;
        synchronized (classData.normalValues) {
            normalValues = (IntFlagStringValuePair[]) classData.normalValues.toArray(
                new IntFlagStringValuePair[classData.normalValues.size()]);
        }

        final List values = new ArrayList();

        boolean foundExactSpecialValue = false;
        for (int i = 0; i < specialValues.length; i++) {
            if (flags == specialValues[i].flag) {
                // If our flags are exactly equal to a special flag then return
                // that and only that value.
                values.clear();
                values.add(specialValues[i].value);
                foundExactSpecialValue = true;
                break;
            }
            if (specialValues[i].flag != 0 && ((flags & specialValues[i].flag) == specialValues[i].flag)) {
                values.add(specialValues[i].value);
            }
        }
        if (!foundExactSpecialValue) {
            for (int i = 0; i < normalValues.length; i++) {
                if ((flags & normalValues[i].flag) != 0) {
                    values.add(normalValues[i].value);
                }
            }
        }
        return (String[]) values.toArray(new String[values.size()]);
    }

    private String[] toStringValues(final int flags, final Class key) {
        BitFieldStringData classData;

        synchronized (PER_CLASS_DATA) {
            classData = (BitFieldStringData) PER_CLASS_DATA.get(key);
        }

        if (classData == null) {
            return null;
        }

        IntFlagStringValuePair[] specialValues;
        synchronized (classData.specialValues) {
            specialValues = (IntFlagStringValuePair[]) classData.specialValues.toArray(
                new IntFlagStringValuePair[classData.specialValues.size()]);
        }

        for (int i = 0; i < specialValues.length; i++) {
            if (specialValues[i].flag == flags) {
                return new String[] {
                    specialValues[i].value
                };
            }
        }

        IntFlagStringValuePair[] normalValues;
        synchronized (classData.normalValues) {
            normalValues = (IntFlagStringValuePair[]) classData.normalValues.toArray(
                new IntFlagStringValuePair[classData.normalValues.size()]);
        }

        final List values = new ArrayList();

        for (int i = 0; i < normalValues.length; i++) {
            if ((flags & normalValues[i].flag) != 0) {
                values.add(normalValues[i].value);
            }
        }

        return (String[]) values.toArray(new String[values.size()]);
    }

    protected static int fromStringValues(final String[] strings, final Class key) {
        if (strings.length == 0) {
            return 0;
        }

        BitFieldStringData classData;

        synchronized (PER_CLASS_DATA) {
            classData = (BitFieldStringData) PER_CLASS_DATA.get(key);
        }

        if (classData == null) {
            return 0;
        }

        int flags = 0;

        /*
         * Check for exact matches against special values.
         */

        IntFlagStringValuePair[] specialValues;
        synchronized (classData.specialValues) {
            specialValues = (IntFlagStringValuePair[]) classData.specialValues.toArray(
                new IntFlagStringValuePair[classData.specialValues.size()]);
        }

        for (int i = 0; i < strings.length; i++) {
            for (int j = 0; j < specialValues.length; j++) {
                if (specialValues[j].value.equals(strings[i])) {
                    flags |= specialValues[j].flag;
                }
            }
        }

        /*
         * Check for matches against all known normal values, accumulate them in
         * flags.
         */
        IntFlagStringValuePair[] normalValues;
        synchronized (classData.normalValues) {
            normalValues = (IntFlagStringValuePair[]) classData.normalValues.toArray(
                new IntFlagStringValuePair[classData.normalValues.size()]);
        }

        for (int i = 0; i < strings.length; i++) {
            for (int j = 0; j < normalValues.length; j++) {
                if (normalValues[j].value.equals(strings[i])) {
                    flags |= normalValues[j].flag;
                }
            }
        }

        return flags;
    }

    protected static int combine(final BitField[] instances) {
        Check.notNull(instances, "instances"); //$NON-NLS-1$

        int flags = 0;
        for (int i = 0; i < instances.length; i++) {
            if (instances[i] == null) {
                throw new IllegalArgumentException("array element " + i + " was null"); //$NON-NLS-1$ //$NON-NLS-2$
            }
            flags |= instances[i].flags;
        }
        return flags;
    }

    protected static void registerStringValue(final Class key, final int flagValue, final String stringValue) {
        registerStringValue(key, flagValue, stringValue, false);
    }

    protected static void registerStringValue(
        final Class key,
        final int flagValue,
        final String stringValue,
        final boolean forceSpecial) {
        Check.notNull(key, "key"); //$NON-NLS-1$
        Check.notNull(stringValue, "stringValue"); //$NON-NLS-1$

        final IntFlagStringValuePair pair = new IntFlagStringValuePair(flagValue, stringValue);

        BitFieldStringData classData;

        synchronized (PER_CLASS_DATA) {
            classData = (BitFieldStringData) PER_CLASS_DATA.get(key);
            if (classData == null) {
                classData = new BitFieldStringData();
                PER_CLASS_DATA.put(key, classData);
            }
        }

        final boolean specialValue = forceSpecial || (flagValue == 0) || (bitCount(flagValue) > 1);

        if (specialValue) {
            synchronized (classData.specialValues) {
                classData.specialValues.add(pair);
            }
        } else {
            synchronized (classData.normalValues) {
                classData.normalValues.add(pair);
            }
        }
    }

    /**
     * @return the combined value of all the special flags set in this
     *         {@link BitField}.
     */
    protected int getCombinedSpecialFlags() {
        BitFieldStringData classData;

        synchronized (PER_CLASS_DATA) {
            classData = (BitFieldStringData) PER_CLASS_DATA.get(getClass());
        }

        if (classData == null) {
            return 0;
        }

        IntFlagStringValuePair[] specialValues;
        synchronized (classData.specialValues) {
            specialValues = (IntFlagStringValuePair[]) classData.specialValues.toArray(
                new IntFlagStringValuePair[classData.specialValues.size()]);
        }

        int specialFlags = 0;

        /*
         * For each of the special values for this type, if the special value is
         * set in our flags, combine that special value with the return value.
         */
        for (int j = 0; j < specialValues.length; j++) {
            if ((flags & specialValues[j].flag) == specialValues[j].flag) {
                specialFlags |= specialValues[j].flag;
            }
        }

        return specialFlags;
    }

    private static final Map PER_CLASS_DATA = new HashMap();

    private static class BitFieldStringData {
        public final List specialValues = new ArrayList();
        public final List normalValues = new ArrayList();
    }

    private static class IntFlagStringValuePair {
        public final int flag;
        public final String value;

        public IntFlagStringValuePair(final int flag, final String value) {
            this.flag = flag;
            this.value = value;
        }
    }

    /**
     * Counts the number of bits that are set in the specified integer flag
     * value.
     *
     * @param flagValue
     *        the value to count bits on
     * @return the number of bits set
     */
    private static int bitCount(int flagValue) {
        /*
         * See: http://graphics.stanford.edu/~seander/bithacks.html#
         * CountBitsSetKernighan
         */

        int count;
        for (count = 0; flagValue != 0; count++) {
            flagValue &= flagValue - 1; // clear the least significant bit set
        }

        return count;
    }
}
