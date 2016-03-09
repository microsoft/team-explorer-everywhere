// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.metadata.dao;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.microsoft.tfs.core.internal.db.DBConnection;

public class FieldDefinitionMetadata {
    public static String getSelectStatement(final DBConnection connection) {
        final StringBuilder sb = new StringBuilder();
        sb.append("select FldID, Type, Name, ReferenceName"); //$NON-NLS-1$

        // fSupportsTextQuery added in TFS 2012 Beta3
        if (connection.getDBSpecificOperations().columnExists("Fields", "fSupportsTextQuery")) //$NON-NLS-1$ //$NON-NLS-2$
        {
            sb.append(", fSupportsTextQuery"); //$NON-NLS-1$
        }

        sb.append(" from Fields"); //$NON-NLS-1$
        return sb.toString();
    }

    public static FieldDefinitionMetadata fromRow(final ResultSet rset) throws SQLException {
        final int id = rset.getInt(1);
        final int type = rset.getInt(2);
        final String name = rset.getString(3);
        final String referenceName = rset.getString(4);
        final boolean supportsTextQuery = rset.getMetaData().getColumnCount() > 4 ? rset.getBoolean(5) : false;

        return new FieldDefinitionMetadata(id, type, name, referenceName, supportsTextQuery);
    }

    private final int id;
    private final int type;
    private final String name;
    private final String referenceName;
    private final boolean supportsTextQuery;

    public FieldDefinitionMetadata(
        final int id,
        final int type,
        final String name,
        final String referenceName,
        final boolean supportsTextQuery) {
        this.id = id;
        this.type = type;
        this.name = name;
        this.referenceName = referenceName;
        this.supportsTextQuery = supportsTextQuery;
    }

    public int getID() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getReferenceName() {
        return referenceName;
    }

    public int getType() {
        return type;
    }

    public boolean supportsTextQuery() {
        return supportsTextQuery;
    }
}
