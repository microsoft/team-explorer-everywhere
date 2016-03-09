// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.metadata.dao;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.microsoft.tfs.core.internal.db.DBConnection;

public class FieldUsageMetadata {
    public static String getSelectStatement(final DBConnection connection) {
        final StringBuilder sb = new StringBuilder();
        sb.append("select FldID, fCore, fOftenQueried"); //$NON-NLS-1$

        // fSupportsTextQuery added in TFS 2012 Beta3
        if (connection.getDBSpecificOperations().columnExists("FieldUsages", "fSupportsTextQuery")) //$NON-NLS-1$ //$NON-NLS-2$
        {
            sb.append(", fSupportsTextQuery"); //$NON-NLS-1$
        }

        sb.append(" from FieldUsages"); //$NON-NLS-1$
        return sb.toString();
    }

    public static FieldUsageMetadata fromRow(final ResultSet rset) throws SQLException {
        final int fieldId = rset.getInt(1);
        final boolean core = rset.getBoolean(2);
        final boolean oftenQueried = rset.getBoolean(3);
        final boolean supportsTextQuery = rset.getMetaData().getColumnCount() > 3 ? rset.getBoolean(4) : false;

        return new FieldUsageMetadata(fieldId, core, oftenQueried, supportsTextQuery);
    }

    private final int fieldId;
    private final boolean core;
    private final boolean oftenQueried;
    private final boolean supportsTextQuery;

    public FieldUsageMetadata(
        final int fieldId,
        final boolean core,
        final boolean oftenQueried,
        final boolean supportsTextQuery) {
        this.fieldId = fieldId;
        this.core = core;
        this.oftenQueried = oftenQueried;
        this.supportsTextQuery = supportsTextQuery;
    }

    public boolean isCore() {
        return core;
    }

    public int getFieldID() {
        return fieldId;
    }

    public boolean isOftenQueried() {
        return oftenQueried;
    }

    public boolean supportsTextQuery() {
        return supportsTextQuery;
    }
}
