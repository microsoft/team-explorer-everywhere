// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.internal.db;

public interface DBSpecificOperations {
    public boolean tableExists(String tableName);

    public boolean columnExists(String tableName, String columnName);
}
