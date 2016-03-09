// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.rowset;

import java.util.HashSet;
import java.util.Set;

/**
 * A table handler that handles the table returned from the GetStoredQueryItems
 * call.
 */
public class GetStoredQueryItemsRowSetHandler extends BaseRowSetHandler {
    private final Set<GetStoredQueryItemRowSet> storedQueryItems = new HashSet<GetStoredQueryItemRowSet>();

    public GetStoredQueryItemsRowSetHandler() {
    }

    @Override
    public void handleBeginParsing() {
        super.handleBeginParsing();
        storedQueryItems.clear();
    }

    @Override
    protected void doHandleRow() {
        final GetStoredQueryItemRowSet query = new GetStoredQueryItemRowSet(
            getStringValue(GetStoredQueryItemsFieldNames.ID),
            getStringValue(GetStoredQueryItemsFieldNames.NAME),
            getStringValue(GetStoredQueryItemsFieldNames.TEXT),
            getStringValue(GetStoredQueryItemsFieldNames.OWNER_IDENTIFIER),
            getStringValue(GetStoredQueryItemsFieldNames.OWNER_TYPE),
            getStringValue(GetStoredQueryItemsFieldNames.PARENT_ID),
            getBooleanValue(GetStoredQueryItemsFieldNames.FOLDER),
            getBooleanValue(GetStoredQueryItemsFieldNames.DELETED),
            getLongValue(GetStoredQueryItemsFieldNames.CACHESTAMP));

        storedQueryItems.add(query);
    }

    public GetStoredQueryItemRowSet[] getRowSets() {
        return storedQueryItems.toArray(new GetStoredQueryItemRowSet[] {});
    }
}
