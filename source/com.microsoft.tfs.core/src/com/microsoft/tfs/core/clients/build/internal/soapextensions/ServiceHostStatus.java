// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.internal.soapextensions;

import com.microsoft.tfs.core.internal.wrappers.EnumerationWrapper;

import ms.tfs.build.buildservice._04._ServiceHostStatus;

public class ServiceHostStatus extends EnumerationWrapper {
    public static final ServiceHostStatus OFFLINE = new ServiceHostStatus(_ServiceHostStatus.Offline);
    public static final ServiceHostStatus ONLINE = new ServiceHostStatus(_ServiceHostStatus.Online);

    private ServiceHostStatus(final _ServiceHostStatus type) {
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
    public static ServiceHostStatus fromWebServiceObject(final _ServiceHostStatus webServiceObject) {
        if (webServiceObject == null) {
            return null;
        }
        return (ServiceHostStatus) EnumerationWrapper.fromWebServiceObject(webServiceObject);
    }

    /**
     * Gets the web service object this class wraps. The returned object should
     * not be modified.
     *
     * @return the web service object this class wraps.
     */
    public _ServiceHostStatus getWebServiceObject() {
        return (_ServiceHostStatus) webServiceObject;
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
