// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.internal.db;

public class MSSQLOperations implements DBSpecificOperations {
    private final DBConnection connection;

    public MSSQLOperations(final DBConnection connection) {
        this.connection = connection;
    }

    @Override
    public boolean tableExists(final String tableName) {
        final DBStatement stmt =
            connection.createStatement("select count(*) from INFORMATION_SCHEMA.tables where table_name = ?"); //$NON-NLS-1$
        return stmt.executeIntQuery(new Object[] {
            tableName
        }).intValue() > 0;
    }

    @Override
    public boolean columnExists(final String tableName, final String columnName) {
        final DBStatement stmt = connection.createStatement(
            "select count(*) from INFORMATION_SCHEMA.columns where table_name = ? and column_name = ?"); //$NON-NLS-1$
        return stmt.executeIntQuery(new Object[] {
            tableName,
            columnName
        }).intValue() > 0;
    }
}
