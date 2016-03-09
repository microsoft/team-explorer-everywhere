// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.query;

import com.microsoft.tfs.core.clients.workitem.internal.query.StoredQueryImpl;

/**
 * Factory for {@link StoredQuery} objects.
 *
 * @since TEE-SDK-10.1
 */
public class StoredQueryFactory {
    public static StoredQuery newStoredQuery(
        final QueryScope queryScope,
        final String queryName,
        final String queryText,
        final String description) throws InvalidQueryTextException {
        return new StoredQueryImpl(queryScope, queryName, queryText, description);
    }
}
