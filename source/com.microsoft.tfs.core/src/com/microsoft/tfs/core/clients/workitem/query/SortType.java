// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.query;

/**
 * Describes the order by which a {@link SortField} is sorted.
 *
 * @since TEE-SDK-10.1
 */
public class SortType {
    public static final SortType ASCENDING = new SortType(0);
    public static final SortType DESCENDING = new SortType(1);

    private final int sortType;

    private SortType(final int sortType) {
        this.sortType = sortType;
    }

    @Override
    public String toString() {
        return String.valueOf(sortType);
    }

    public int getSortType() {
        return sortType;
    }
}
