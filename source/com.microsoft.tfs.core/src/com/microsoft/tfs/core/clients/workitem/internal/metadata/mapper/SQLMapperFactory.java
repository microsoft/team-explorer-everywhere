// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.metadata.mapper;

import java.text.MessageFormat;

import com.microsoft.tfs.core.internal.db.DBConnection;

public class SQLMapperFactory {
    public static SQLMapper getSQLMapper(final DBConnection connection) {
        if (connection.getDriverClass().equals("org.hsqldb.jdbcDriver")) //$NON-NLS-1$
        {
            return new HSQLMapper();
        } else if (connection.getDriverClass().equals("net.sourceforge.jtds.jdbc.Driver")) //$NON-NLS-1$
        {
            return new MSSQLMapper();
        } else {
            throw new RuntimeException(MessageFormat.format("unknown driver class [{0}]", connection.getDriverClass())); //$NON-NLS-1$
        }
    }
}
