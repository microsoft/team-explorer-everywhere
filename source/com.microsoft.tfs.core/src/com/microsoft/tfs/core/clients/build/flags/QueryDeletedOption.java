// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.flags;

import com.microsoft.tfs.core.internal.wrappers.EnumerationWrapper;

import ms.tfs.build.buildservice._04._QueryDeletedOption;

/**
 * Options for querying deleted items.
 *
 * @since TEE-SDK-10.1
 */
public class QueryDeletedOption extends EnumerationWrapper {

    /**
     * Include only non-deleted builds.
     */
    public static final QueryDeletedOption EXCLUDE_DELETED = new QueryDeletedOption(_QueryDeletedOption.ExcludeDeleted);

    /**
     * Include deleted and non-deleted builds.
     */
    public static final QueryDeletedOption INCLUDE_DELETED = new QueryDeletedOption(_QueryDeletedOption.IncludeDeleted);

    /**
     * Include only deleted builds.
     */
    public static final QueryDeletedOption ONLY_DELETED = new QueryDeletedOption(_QueryDeletedOption.OnlyDeleted);

    private QueryDeletedOption(final _QueryDeletedOption type) {
        super(type);
    }

    /**
     * Gets the correct wrapper type for the given web service object.
     *
     * @param webServiceObject
     *        the web service object (must not be <code>null</code>)
     * @return the correct wrapper type for the given web service object
     * @throws RuntimeException
     *         if no wrapper type is known for the given web service object
     */
    public static QueryDeletedOption fromWebServiceObject(final _QueryDeletedOption webServiceObject) {
        if (webServiceObject == null) {
            return null;
        }
        return (QueryDeletedOption) EnumerationWrapper.fromWebServiceObject(webServiceObject);
    }

    /**
     * Gets the web service object this class wraps. The returned object should
     * not be modified.
     *
     * @return the web service object this class wraps.
     */
    public _QueryDeletedOption getWebServiceObject() {
        return (_QueryDeletedOption) webServiceObject;
    }

    @Override
    public boolean equals(final Object o) {
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
