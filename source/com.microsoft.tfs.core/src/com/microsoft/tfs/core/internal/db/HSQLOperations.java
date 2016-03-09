// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.internal.db;

import java.util.Locale;

public class HSQLOperations implements DBSpecificOperations {
    private final DBConnection connection;

    public HSQLOperations(final DBConnection connection) {
        this.connection = connection;
    }

    @Override
    public boolean tableExists(final String tableName) {
        final DBStatement stmt =
            connection.createStatement("select count(*) from INFORMATION_SCHEMA.SYSTEM_TABLES where TABLE_NAME = ?"); //$NON-NLS-1$
        return stmt.executeNumericQuery(new Object[] {
            tableName.toUpperCase(Locale.US)
        }) > 0;
    }

    @Override
    public boolean columnExists(final String tableName, final String columnName) {
        final DBStatement stmt = connection.createStatement(
            "select count(*) from INFORMATION_SCHEMA.SYSTEM_COLUMNS where TABLE_NAME = ? and COLUMN_NAME = ?"); //$NON-NLS-1$
        return stmt.executeNumericQuery(new Object[] {
            tableName.toUpperCase(Locale.US),
            columnName.toUpperCase(Locale.US)
        }) > 0;
    }
}
