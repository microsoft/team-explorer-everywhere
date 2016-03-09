// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.authorization;

import com.microsoft.tfs.core.internal.wrappers.EnumerationWrapper;

import ms.tfs.services.authorization._03._QueryMembership;

/**
 * Specifies the constants that specify the level of detail returned in a query.
 *
 * @since TEE-SDK-10.1
 */
public class QueryMembership extends EnumerationWrapper {
    public static final QueryMembership NONE = new QueryMembership(_QueryMembership.None);
    public static final QueryMembership DIRECT = new QueryMembership(_QueryMembership.Direct);
    public static final QueryMembership EXPANDED = new QueryMembership(_QueryMembership.Expanded);

    private QueryMembership(final _QueryMembership state) {
        super(state);
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
    public static QueryMembership fromWebServiceObject(final _QueryMembership webServiceObject) {
        return (QueryMembership) EnumerationWrapper.fromWebServiceObject(webServiceObject);
    }

    /**
     * Gets the web service object this class wraps. The returned object should
     * not be modified.
     *
     * @return the web service object this class wraps.
     */
    public _QueryMembership getWebServiceObject() {
        return (_QueryMembership) webServiceObject;
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
