// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.fields;

import java.util.Date;

import com.microsoft.tfs.util.GUID;

/**
 * Describes the data type of a {@link Field}.
 *
 * @since TEE-SDK-10.1
 * @threadsafety immutable
 */
public class FieldType {
    public static final FieldType INTERNAL = new FieldType("Internal", 0); //$NON-NLS-1$

    /**
     * The field is a {@link String}.
     */
    public static final FieldType STRING = new FieldType("String", 1); //$NON-NLS-1$

    /**
     * The field is an {@link Integer}.
     */
    public static final FieldType INTEGER = new FieldType("Integer", 2); //$NON-NLS-1$

    /**
     * The field is a {@link Date} object.
     */
    public static final FieldType DATETIME = new FieldType("DateTime", 3); //$NON-NLS-1$

    /**
     * The field is a {@link String}.
     */
    public static final FieldType PLAINTEXT = new FieldType("PlainText", 5); //$NON-NLS-1$

    /**
     * The field is a {@link String}.
     */
    public static final FieldType HTML = new FieldType("Html", 7); //$NON-NLS-1$

    /**
     * The field is a {@link String}.
     */
    public static final FieldType TREEPATH = new FieldType("TreePath", 8); //$NON-NLS-1$

    /**
     * The field is a {@link String}.
     */
    public static final FieldType HISTORY = new FieldType("History", 9); //$NON-NLS-1$

    /**
     * The field is a {@link Double}.
     */
    public static final FieldType DOUBLE = new FieldType("Double", 10); //$NON-NLS-1$

    /**
     * The field is a {@link GUID}.
     */
    public static final FieldType GUID = new FieldType("Guid", 11); //$NON-NLS-1$

    /**
     * The field is a {@link Boolean}.
     */
    public static final FieldType BOOLEAN = new FieldType("Boolean", 12); //$NON-NLS-1$

    private final String type;
    private final int value;

    private FieldType(final String type, final int value) {
        this.type = type;
        this.value = value;
    }

    public String getDisplayName() {
        return type;
    }

    public int getValue() {
        return value;
    }

}
