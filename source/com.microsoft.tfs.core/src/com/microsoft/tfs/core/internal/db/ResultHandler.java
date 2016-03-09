// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.internal.db;

import java.sql.ResultSet;
import java.sql.SQLException;

public interface ResultHandler {
    public void handleRow(ResultSet rset) throws SQLException;
}
