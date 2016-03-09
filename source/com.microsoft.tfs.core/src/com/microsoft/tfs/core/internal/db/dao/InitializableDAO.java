// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.internal.db.dao;

public interface InitializableDAO {
    public void initialize(DBConnectionSource connectionSource);
}
