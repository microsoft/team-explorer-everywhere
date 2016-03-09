// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.internal.db.dao;

import com.microsoft.tfs.core.internal.db.DBConnection;

public class BaseDAOImplementation implements InitializableDAO {
    private DBConnectionSource connectionSource;

    @Override
    public void initialize(final DBConnectionSource connectionSource) {
        this.connectionSource = connectionSource;
    }

    protected DBConnection getConnection() {
        return connectionSource.getConnection();
    }
}
