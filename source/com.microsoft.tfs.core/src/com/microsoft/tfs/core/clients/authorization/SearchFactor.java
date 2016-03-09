// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.authorization;

import com.microsoft.tfs.core.internal.wrappers.EnumerationWrapper;

import ms.tfs.services.authorization._03._SearchFactor;

/**
 * Specifies an attribute of an identity to be used in a search.
 *
 * @since TEE-SDK-10.1
 */
public class SearchFactor extends EnumerationWrapper {
    public static final SearchFactor NONE = new SearchFactor(_SearchFactor.None);
    public static final SearchFactor SID = new SearchFactor(_SearchFactor.Sid);
    public static final SearchFactor ACCOUNT_NAME = new SearchFactor(_SearchFactor.AccountName);
    public static final SearchFactor DISTINGUISHED_NAME = new SearchFactor(_SearchFactor.DistinguishedName);
    public static final SearchFactor ADMINISTRATIVE_APPLICATION_GROUP =
        new SearchFactor(_SearchFactor.AdministrativeApplicationGroup);
    public static final SearchFactor SERVICE_APPLICATION_GROUP =
        new SearchFactor(_SearchFactor.ServiceApplicationGroup);
    public static final SearchFactor EVERYONE_APPLICATION_GROUP =
        new SearchFactor(_SearchFactor.EveryoneApplicationGroup);

    private SearchFactor(final _SearchFactor state) {
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
    public static SearchFactor fromWebServiceObject(final _SearchFactor webServiceObject) {
        return (SearchFactor) EnumerationWrapper.fromWebServiceObject(webServiceObject);
    }

    /**
     * Gets the web service object this class wraps. The returned object should
     * not be modified.
     *
     * @return the web service object this class wraps.
     */
    public _SearchFactor getWebServiceObject() {
        return (_SearchFactor) webServiceObject;
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
