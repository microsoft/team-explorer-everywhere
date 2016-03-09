// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.soapextensions;

import com.microsoft.tfs.core.internal.wrappers.EnumerationWrapper;

import ms.tfs.versioncontrol.clientservices._03._RecursionType;

/**
 * Enumerates the kinds of recursion available for version control items.
 *
 * @since TEE-SDK-10.1
 */
public class RecursionType extends EnumerationWrapper {
    public static final RecursionType NONE = new RecursionType(_RecursionType.None);
    public static final RecursionType ONE_LEVEL = new RecursionType(_RecursionType.OneLevel);
    public static final RecursionType FULL = new RecursionType(_RecursionType.Full);

    private RecursionType(final _RecursionType recursionType) {
        super(recursionType);
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
    public static RecursionType fromWebServiceObject(final _RecursionType webServiceObject) {
        return (RecursionType) EnumerationWrapper.fromWebServiceObject(webServiceObject);
    }

    /**
     * Gets the web service object this class wraps. The returned object should
     * not be modified.
     *
     * @return the web service object this class wraps.
     */
    public _RecursionType getWebServiceObject() {
        return (_RecursionType) webServiceObject;
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
