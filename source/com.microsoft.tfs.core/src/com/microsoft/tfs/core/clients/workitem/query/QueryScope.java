// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.query;

/**
 * Describes the query scope.
 *
 * @since TEE-SDK-10.1
 */
public class QueryScope {
    public static final QueryScope PRIVATE = new QueryScope(2);
    public static final QueryScope PUBLIC = new QueryScope(1);

    private final int value;

    private QueryScope(final int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    @Override
    public String toString() {
        return (this == QueryScope.PRIVATE ? "PRIVATE" : "PUBLIC"); //$NON-NLS-1$ //$NON-NLS-2$
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof QueryScope) {
            final QueryScope other = (QueryScope) obj;
            return value == other.value;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return value;
    }
}
