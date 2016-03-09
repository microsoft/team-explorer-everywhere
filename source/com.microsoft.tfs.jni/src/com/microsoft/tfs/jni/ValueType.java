// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.jni;

/**
 * Defines type ID and display name for supported Windows registry types.
 *
 *
 * @threadsafety unknown
 */
public class ValueType {
    public static ValueType REG_SZ = new ValueType(0x1, "REG_SZ"); //$NON-NLS-1$
    public static ValueType REG_DWORD = new ValueType(0x4, "REG_DWORD"); //$NON-NLS-1$

    private final int value;
    private final String displayValue;

    private ValueType(final int value, final String displayValue) {
        this.value = value;
        this.displayValue = displayValue;
    }

    public int getValue() {
        return value;
    }

    @Override
    public String toString() {
        return displayValue;
    }
}
