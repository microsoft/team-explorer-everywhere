// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.update;

import com.microsoft.tfs.core.clients.workitem.internal.WITContext;
import com.microsoft.tfs.core.clients.workitem.queryhierarchy.QueryItem;
import com.microsoft.tfs.core.ws.runtime.types.DOMAnyContentType;

/**
 * A metapackage which acts as a container for batching updates.
 */
public class QueryHierarchyBatchUpdatePackage extends BaseUpdatePackage {
    public QueryHierarchyBatchUpdatePackage(final WITContext context) {
        super(context);
    }

    public void insertQueryItem(final QueryItem queryItem) {
        InsertQueryItemUpdatePackage.populate(getRoot(), queryItem);
    }

    public void updateQueryItem(final QueryItem queryItem) {
        UpdateQueryItemUpdatePackage.populate(getRoot(), queryItem);
    }

    public void deleteQueryItem(final QueryItem queryItem) {
        DeleteQueryItemUpdatePackage.populate(getRoot(), queryItem);
    }

    @Override
    protected void handleUpdateResponse(final DOMAnyContentType response) {
        /* Query hierarchy ignores non-error responses from the server */
    }
}
