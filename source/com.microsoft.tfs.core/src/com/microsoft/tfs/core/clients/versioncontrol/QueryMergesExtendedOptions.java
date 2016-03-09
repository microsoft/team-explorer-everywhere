// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol;

import com.microsoft.tfs.util.BitField;

/**
 * Options that affect how 2010 merges are queried.
 *
 * @threadsafety thread-safe
 * @since TEE-SDK-10.1
 */
public final class QueryMergesExtendedOptions extends BitField {
    public static final QueryMergesExtendedOptions NONE = new QueryMergesExtendedOptions(0, "None"); //$NON-NLS-1$

    /**
     * Specifies the query should only query sources of a rename. Specifies the
     * query should only query source / targets of a rename. The default is to
     * query sources of a merge. The default is to query source / targets of a
     * merge.
     */
    public static final QueryMergesExtendedOptions QUERY_RENAMES = new QueryMergesExtendedOptions(1, "QueryRenames"); //$NON-NLS-1$

    /**
     * Include download info.
     */
    public static final QueryMergesExtendedOptions INCLUDE_DOWNLOAD_INFO =
        new QueryMergesExtendedOptions(2, "IncludeDownloadInfo"); //$NON-NLS-1$

    /**
     * Specifies that the query should query targets for a given source item.
     */
    public static final QueryMergesExtendedOptions QUERY_TARGET_MERGES =
        new QueryMergesExtendedOptions(4, "QueryTargetMerges"); //$NON-NLS-1$

    public static QueryMergesExtendedOptions combine(final QueryMergesExtendedOptions[] changeTypes) {
        return new QueryMergesExtendedOptions(BitField.combine(changeTypes));
    }

    private QueryMergesExtendedOptions(final int flags, final String name) {
        super(flags);
        registerStringValue(getClass(), flags, name);
    }

    private QueryMergesExtendedOptions(final int flags) {
        super(flags);
    }

    public boolean containsAll(final QueryMergesExtendedOptions other) {
        return containsAllInternal(other);
    }

    public boolean contains(final QueryMergesExtendedOptions other) {
        return containsInternal(other);
    }

    public boolean containsAny(final QueryMergesExtendedOptions other) {
        return containsAnyInternal(other);
    }

    public QueryMergesExtendedOptions remove(final QueryMergesExtendedOptions other) {
        return new QueryMergesExtendedOptions(removeInternal(other));
    }

    public QueryMergesExtendedOptions retain(final QueryMergesExtendedOptions other) {
        return new QueryMergesExtendedOptions(retainInternal(other));
    }

    public QueryMergesExtendedOptions combine(final QueryMergesExtendedOptions other) {
        return new QueryMergesExtendedOptions(combineInternal(other));
    }
}