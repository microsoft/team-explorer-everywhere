// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.internal.db;

import java.sql.Connection;
import java.sql.SQLException;
import java.text.MessageFormat;

public class DBConnection {
    private final Connection connection;
    private final String driverClass;
    private DBSpecificOperations dbSpecificOperations;

    public DBConnection(final Connection connection, final String driverClass) {
        this.connection = connection;
        this.driverClass = driverClass;

        if (driverClass.equals("org.hsqldb.jdbcDriver")) //$NON-NLS-1$
        {
            dbSpecificOperations = new HSQLOperations(this);
        } else if (driverClass.equals("net.sourceforge.jtds.jdbc.Driver")) //$NON-NLS-1$
        {
            dbSpecificOperations = new MSSQLOperations(this);
        } else {
            throw new IllegalArgumentException(MessageFormat.format("unknown driver class [{0}]", driverClass)); //$NON-NLS-1$
        }
    }

    public String getDriverClass() {
        return driverClass;
    }

    public DBStatement createStatement(final String sql) {
        return new DBStatement(connection, sql);
    }

    public void close() {
        try {
            connection.close();
        } catch (final SQLException e) {
            throw new DBException(e);
        }
    }

    public DBSpecificOperations getDBSpecificOperations() {
        return dbSpecificOperations;
    }
}
