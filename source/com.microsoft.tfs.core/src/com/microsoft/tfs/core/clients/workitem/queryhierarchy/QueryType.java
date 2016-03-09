// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.queryhierarchy;

import com.microsoft.tfs.util.TypesafeEnum;

/**
 * Defines types of work item queries.
 *
 * @since TEE-SDK-10.1
 */
public final class QueryType extends TypesafeEnum {
    /**
     * A query that cannot be resolved as well-formed WIQL will not have a valid
     * {@link QueryType}.
     */
    public static final QueryType INVALID = new QueryType(0);

    /**
     * A flat list of all work items that match the given WIQL query.
     */
    public static final QueryType LIST = new QueryType(1);

    /**
     * A list of all work items and immediate children that match the given WIQL
     * query.
     */
    public static final QueryType ONE_HOP = new QueryType(2);

    /**
     * A list of all work items and all children that match the given WIQL
     * query.
     */
    public static final QueryType TREE = new QueryType(3);

    private QueryType(final int type) {
        super(type);
    }
}
