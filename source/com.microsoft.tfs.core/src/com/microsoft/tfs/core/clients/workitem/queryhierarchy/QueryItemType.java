// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.queryhierarchy;

import com.microsoft.tfs.util.BitField;

/**
 * Defines types of {@link QueryItem}s.
 *
 * @since TEE-SDK-10.1
 */
public final class QueryItemType extends BitField {
    private static final long serialVersionUID = 136908144101610046L;

    /**
     * A Team Project node in the query hierarchy tree.
     */
    public static final QueryItemType PROJECT = new QueryItemType(1);

    /**
     * A folder in the query hierarchy tree.
     */
    public static final QueryItemType QUERY_FOLDER = new QueryItemType(2);

    /**
     * A query definition (stored query) in the query hierarchy tree.
     */
    public static final QueryItemType QUERY_DEFINITION = new QueryItemType(4);

    /**
     * Useful shorthand for querying all item types (Team Projects, query
     * folders and query definitions.)
     */
    public static final QueryItemType ALL = new QueryItemType(QueryItemType.combine(new QueryItemType[] {
        QueryItemType.PROJECT,
        QueryItemType.QUERY_FOLDER,
        QueryItemType.QUERY_DEFINITION
    }));

    /**
     * Useful shorthand for querying all folder types (Team Projects and query
     * folders.)
     */
    public static final QueryItemType ALL_FOLDERS = new QueryItemType(QueryItemType.combine(new QueryItemType[] {
        QueryItemType.PROJECT,
        QueryItemType.QUERY_FOLDER
    }));

    private QueryItemType(final int flags) {
        super(flags);
    }

    public boolean contains(final QueryItemType type) {
        return containsInternal(type);
    }
}
