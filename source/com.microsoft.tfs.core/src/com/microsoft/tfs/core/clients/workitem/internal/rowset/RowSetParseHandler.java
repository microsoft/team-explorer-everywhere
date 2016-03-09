// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.rowset;

public interface RowSetParseHandler {
    public void handleBeginParsing();

    public void handleTableName(String tableName);

    public void handleColumn(String name, String type);

    public void handleFinishedColumns();

    public void handleRow(String[] rowValues);

    public void handleEndParsing();
}
