// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.flags;

import com.microsoft.tfs.util.BitField;

import ms.tfs.build.buildservice._04._QueryOptions;
import ms.tfs.build.buildservice._04._QueryOptions._QueryOptions_Flag;

/**
 * Options for a query.
 *
 * @since TEE-SDK-10.1
 */
@SuppressWarnings("serial")
public final class QueryOptions extends BitField {
    public static final QueryOptions NONE = new QueryOptions(0, _QueryOptions_Flag.None);
    public static final QueryOptions DEFINITIONS = new QueryOptions(1, _QueryOptions_Flag.Definitions);
    public static final QueryOptions AGENTS = new QueryOptions(2, _QueryOptions_Flag.Agents);
    public static final QueryOptions WORKSPACES = new QueryOptions(5, _QueryOptions_Flag.Workspaces);
    public static final QueryOptions CONTROLLERS = new QueryOptions(8, _QueryOptions_Flag.Controllers);
    public static final QueryOptions PROCESS = new QueryOptions(17, _QueryOptions_Flag.Process);
    public static final QueryOptions BATCHED_REQUESTS = new QueryOptions(32, _QueryOptions_Flag.BatchedRequests);
    public static final QueryOptions HISTORICAL_BUILDS = new QueryOptions(64, _QueryOptions_Flag.HistoricalBuilds);
    public static final QueryOptions ALL = new QueryOptions(127, _QueryOptions_Flag.All);

    private QueryOptions(final int flags, final _QueryOptions_Flag flag) {
        super(flags);
        registerStringValue(getClass(), flags, flag.toString());
    }

    private QueryOptions(final int flags) {
        super(flags);
    }

    public _QueryOptions getWebServiceObject() {
        return new _QueryOptions(toFullStringValues());
    }

    public static QueryOptions fromWebServiceObject(final _QueryOptions queryOptions) {
        if (queryOptions == null) {
            return null;
        }
        return new QueryOptions(webServiceObjectToFlags(queryOptions));
    }

    private static int webServiceObjectToFlags(final _QueryOptions queryOptions) {
        final _QueryOptions_Flag[] flagArray = queryOptions.getFlags();
        final String[] flagStrings = new String[flagArray.length];
        for (int i = 0; i < flagArray.length; i++) {
            flagStrings[i] = flagArray[i].toString();
        }
        return fromStringValues(flagStrings, QueryOptions.class);
    }

    // -- Common Strongly types BitField methods.

    public static QueryOptions combine(final QueryOptions[] changeTypes) {
        return new QueryOptions(BitField.combine(changeTypes));
    }

    public boolean containsAll(final QueryOptions other) {
        return containsAllInternal(other);
    }

    public boolean contains(final QueryOptions other) {
        return containsInternal(other);
    }

    public boolean containsAny(final QueryOptions other) {
        return containsAnyInternal(other);
    }

    public QueryOptions remove(final QueryOptions other) {
        return new QueryOptions(removeInternal(other));
    }

    public QueryOptions retain(final QueryOptions other) {
        return new QueryOptions(retainInternal(other));
    }

    public QueryOptions combine(final QueryOptions other) {
        return new QueryOptions(combineInternal(other));
    }

}
