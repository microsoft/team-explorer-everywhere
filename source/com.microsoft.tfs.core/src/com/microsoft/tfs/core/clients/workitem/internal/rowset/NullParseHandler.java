// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.rowset;

public class NullParseHandler implements RowSetParseHandler {
    @Override
    public void handleBeginParsing() {
    }

    @Override
    public void handleTableName(final String tableName) {
    }

    @Override
    public void handleColumn(final String name, final String type) {
    }

    @Override
    public void handleFinishedColumns() {
    }

    @Override
    public void handleRow(final String[] rowValues) {
    }

    @Override
    public void handleEndParsing() {
    }
}
