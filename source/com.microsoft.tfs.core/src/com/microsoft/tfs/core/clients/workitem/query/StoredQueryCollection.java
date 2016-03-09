// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.query;

import com.microsoft.tfs.util.GUID;

/**
 * Represents the stored queries collection from a project or work item store.
 *
 * @since TEE-SDK-10.1
 */
public interface StoredQueryCollection {
    public void add(StoredQuery storedQuery) throws InvalidQueryTextException;

    public StoredQuery getQuery(int index);

    public StoredQuery getByGUID(GUID guid);

    public void refresh();

    public void remove(StoredQuery storedQuery);

    public int size();

    // note - below methods do not exist in MS WIT OM

    // if scope is null, public queries are searched first and then private
    // queries
    public StoredQuery getQueryByNameAndScope(String name, QueryScope scope);

    // if scope is null, all queries are returned
    public StoredQuery[] getQueriesByScope(QueryScope scope);
}
