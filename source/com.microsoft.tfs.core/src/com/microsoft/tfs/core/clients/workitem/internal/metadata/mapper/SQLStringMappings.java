// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.metadata.mapper;

public class SQLStringMappings {
    public static int getStringColumnLength(final String tableName, final String columnName) {
        if ("Name".equals(columnName) && "Fields".equals(tableName)) //$NON-NLS-1$ //$NON-NLS-2$
        {
            return 255;
        } else if ("ReferenceName".equals(columnName) && "Fields".equals(tableName)) //$NON-NLS-1$ //$NON-NLS-2$
        {
            return 255;
        } else if ("String".equals(columnName) && "Constants".equals(tableName)) //$NON-NLS-1$ //$NON-NLS-2$
        {
            return 400;
        } else if ("Name".equals(columnName) && "Hierarchy".equals(tableName)) //$NON-NLS-1$ //$NON-NLS-2$
        {
            return 255;
        } else if ("GUID".equals(columnName) && "Hierarchy".equals(tableName)) //$NON-NLS-1$ //$NON-NLS-2$
        {
            return 255;
        } else if ("Name".equals(columnName) && "Actions".equals(tableName)) //$NON-NLS-1$ //$NON-NLS-2$
        {
            return 255;
        } else if ("DisplayName".equals(columnName) && "Constants".equals(tableName)) //$NON-NLS-1$ //$NON-NLS-2$
        {
            return 255;
        }
        return -1;
    }
}
