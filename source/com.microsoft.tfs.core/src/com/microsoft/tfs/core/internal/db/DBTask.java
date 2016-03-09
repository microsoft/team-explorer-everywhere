// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.internal.db;

public interface DBTask {
    public void performTask(DBConnection connection);
}
