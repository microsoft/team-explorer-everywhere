// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.wiqlparse;

public class DataType {
    public static final DataType UNKNOWN = new DataType("UNKNOWN"); //$NON-NLS-1$
    public static final DataType VOID = new DataType("VOID"); //$NON-NLS-1$
    public static final DataType BOOL = new DataType("BOOL"); //$NON-NLS-1$
    public static final DataType NUMERIC = new DataType("NUMERIC"); //$NON-NLS-1$
    public static final DataType DATE = new DataType("DATE"); //$NON-NLS-1$
    public static final DataType STRING = new DataType("STRING"); //$NON-NLS-1$
    public static final DataType TABLE = new DataType("TABLE"); //$NON-NLS-1$
    public static final DataType GUID = new DataType("GUID"); //$NON-NLS-1$

    private final String type;

    private DataType(final String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return type;
    }
}
