// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.internal.db.dao;

import com.microsoft.tfs.core.internal.db.DBConnection;

public interface DBConnectionSource {
    public DBConnection getConnection();
}
