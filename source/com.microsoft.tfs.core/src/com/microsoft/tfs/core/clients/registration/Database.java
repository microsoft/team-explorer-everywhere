// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.registration;

import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;

import ms.tfs.services.registration._03._RegistrationDatabase;

/**
 * Describes a Microsoft SQL Server that is part of the Team Foundation Server
 * installation.
 *
 * @threadsafety thread-compatible
 * @since TEE-SDK-10.1
 */
public class Database extends WebServiceObjectWrapper {
    public Database(
        final String name,
        final String databaseName,
        final String sqlServerName,
        final String connectionString,
        final boolean excludeFromBackup) {
        super(new _RegistrationDatabase(name, databaseName, sqlServerName, connectionString, excludeFromBackup));
    }

    public Database(final _RegistrationDatabase database) {
        super(database);
    }

    /**
     * Gets the web service object this class wraps. The returned object should
     * not be modified.
     *
     * @return the web service object this class wraps.
     */
    public _RegistrationDatabase getWebServiceObject() {
        return (_RegistrationDatabase) webServiceObject;
    }

    public String getName() {
        return getWebServiceObject().getName();
    }

    public String getDatabaseName() {
        return getWebServiceObject().getDatabaseName();
    }

    public String getSQLServerName() {
        return getWebServiceObject().getSQLServerName();
    }

    public String getConnectionString() {
        return getWebServiceObject().getConnectionString();
    }

    public boolean isExcludeFromBackup() {
        return getWebServiceObject().isExcludeFromBackup();
    }
}
