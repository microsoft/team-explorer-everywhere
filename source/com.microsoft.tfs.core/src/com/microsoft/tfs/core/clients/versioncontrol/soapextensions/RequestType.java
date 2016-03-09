// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.soapextensions;

import com.microsoft.tfs.core.internal.wrappers.EnumerationWrapper;

import ms.tfs.versioncontrol.clientservices._03._RequestType;

/**
 * Enumerates the types of requested changes that can be made on version control
 * objects.
 *
 * @since TEE-SDK-10.1
 */
public class RequestType extends EnumerationWrapper {
    public static final RequestType NONE = new RequestType(_RequestType.None);
    public static final RequestType ADD = new RequestType(_RequestType.Add);
    public static final RequestType BRANCH = new RequestType(_RequestType.Branch);
    public static final RequestType ENCODING = new RequestType(_RequestType.Encoding);
    public static final RequestType EDIT = new RequestType(_RequestType.Edit);
    public static final RequestType DELETE = new RequestType(_RequestType.Delete);
    public static final RequestType LOCK = new RequestType(_RequestType.Lock);
    public static final RequestType RENAME = new RequestType(_RequestType.Rename);
    public static final RequestType UNDELETE = new RequestType(_RequestType.Undelete);
    public static final RequestType PROPERTY = new RequestType(_RequestType.Property);

    private RequestType(final _RequestType requestType) {
        super(requestType);
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
    public static RequestType fromWebServiceObject(final _RequestType webServiceObject) {
        return (RequestType) EnumerationWrapper.fromWebServiceObject(webServiceObject);
    }

    /**
     * Gets the web service object this class wraps. The returned object should
     * not be modified.
     *
     * @return the web service object this class wraps.
     */
    public _RequestType getWebServiceObject() {
        return (_RequestType) webServiceObject;
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
