// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.jni;

import com.microsoft.tfs.util.Check;

/**
 * This immutable class holds the data assocated with a Windows registry value.
 * These are returned from get operations and used by set operations). The
 * registry item type is implied based on the constructor used.
 *
 *
 * @threadsafety unknown
 */
public class RegistryValue {
    private final String name;
    private final Object value;
    private final ValueType type;

    public RegistryValue(final String name, final String value) {
        this.name = name;
        this.value = value;
        this.type = ValueType.REG_SZ;
    }

    public RegistryValue(final String name, final int value) {
        this.name = name;
        this.value = new Integer(value);
        this.type = ValueType.REG_DWORD;
    }

    public String getName() {
        return name;
    }

    public ValueType getType() {
        return type;
    }

    public Object getValue() {
        return value;
    }

    public boolean isStringType() {
        return value instanceof String;
    }

    public boolean isIntegerType() {
        return value instanceof Integer;
    }

    public String getStringValue() {
        Check.isTrue(isStringType(), "value must be a string type"); //$NON-NLS-1$

        return (String) value;
    }

    public int getIntegerValue() {
        Check.isTrue(isIntegerType(), "value must be an integer type"); //$NON-NLS-1$

        return ((Integer) value).intValue();
    }
}
