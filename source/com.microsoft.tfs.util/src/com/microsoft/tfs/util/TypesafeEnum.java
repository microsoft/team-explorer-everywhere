// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.util;

/**
 * An implementation of a typesafe enum pattern for Java < 1.5.
 *
 * @threadsafety immutable
 */
public abstract class TypesafeEnum implements Comparable {
    private final int value;

    /**
     * Creates a {@link TypesafeEnum} with the given value.
     *
     * @param value
     *        the integer value for this enumeration
     */
    protected TypesafeEnum(final int value) {
        super();

        this.value = value;
    }

    /**
     * @return the integer value assigned to this enumeration value during
     *         construction
     */
    public int getValue() {
        return value;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj.getClass() != this.getClass()) {
            return false;
        }
        if (obj == this) {
            return true;
        }

        return ((TypesafeEnum) obj).value == value;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int compareTo(final Object otherEnum) {
        final TypesafeEnum other = (TypesafeEnum) otherEnum;

        if (value < other.value) {
            return -1;
        } else if (value > other.value) {
            return 1;
        } else {
            return 0;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return String.valueOf(value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return value;
    }
}
