// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.flags;

import com.microsoft.tfs.util.BitField;

import ms.tfs.build.buildservice._03._QueryOptions;
import ms.tfs.build.buildservice._03._QueryOptions._QueryOptions_Flag;

/**
 * Options for a query.
 *
 * @since TEE-SDK-10.1
 */
public final class QueryOptions2010 extends BitField {
    public static final QueryOptions2010 NONE = new QueryOptions2010(0, _QueryOptions_Flag.None);
    public static final QueryOptions2010 DEFINITIONS = new QueryOptions2010(1, _QueryOptions_Flag.Definitions);
    public static final QueryOptions2010 AGENTS = new QueryOptions2010(2, _QueryOptions_Flag.Agents);
    public static final QueryOptions2010 WORKSPACES = new QueryOptions2010(5, _QueryOptions_Flag.Workspaces);
    public static final QueryOptions2010 CONTROLLERS = new QueryOptions2010(8, _QueryOptions_Flag.Controllers);
    public static final QueryOptions2010 PROCESS = new QueryOptions2010(17, _QueryOptions_Flag.Process);
    public static final QueryOptions2010 ALL = new QueryOptions2010(31, _QueryOptions_Flag.All);

    private QueryOptions2010(final int flags, final _QueryOptions_Flag flag) {
        super(flags);
        registerStringValue(getClass(), flags, flag.toString());
    }

    private QueryOptions2010(final int flags) {
        super(flags);
    }

    public _QueryOptions getWebServiceObject() {
        return new _QueryOptions(toFullStringValues());
    }

    public static QueryOptions2010 fromWebServiceObject(final _QueryOptions queryOptions) {
        if (queryOptions == null) {
            return null;
        }
        return new QueryOptions2010(webServiceObjectToFlags(queryOptions));
    }

    private static int webServiceObjectToFlags(final _QueryOptions queryOptions) {
        final _QueryOptions_Flag[] flagArray = queryOptions.getFlags();
        final String[] flagStrings = new String[flagArray.length];
        for (int i = 0; i < flagArray.length; i++) {
            flagStrings[i] = flagArray[i].toString();
        }
        return fromStringValues(flagStrings, QueryOptions2010.class);
    }

    // -- Common Strongly types BitField methods.

    public static QueryOptions2010 combine(final QueryOptions2010[] changeTypes) {
        return new QueryOptions2010(BitField.combine(changeTypes));
    }

    public boolean containsAll(final QueryOptions2010 other) {
        return containsAllInternal(other);
    }

    public boolean contains(final QueryOptions2010 other) {
        return containsInternal(other);
    }

    public boolean containsAny(final QueryOptions2010 other) {
        return containsAnyInternal(other);
    }

    public QueryOptions2010 remove(final QueryOptions2010 other) {
        return new QueryOptions2010(removeInternal(other));
    }

    public QueryOptions2010 retain(final QueryOptions2010 other) {
        return new QueryOptions2010(retainInternal(other));
    }

    public QueryOptions2010 combine(final QueryOptions2010 other) {
        return new QueryOptions2010(combineInternal(other));
    }

}
