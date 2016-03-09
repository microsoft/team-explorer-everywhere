// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.soapextensions;

import com.microsoft.tfs.core.internal.wrappers.EnumerationWrapper;

import ms.tfs.versioncontrol.clientservices._03._PendingSetType;

/**
 * Defines the type of a {@link PendingSet}.
 *
 * @since TEE-SDK-10.1
 */
public class PendingSetType extends EnumerationWrapper {
    public static final PendingSetType WORKSPACE = new PendingSetType(_PendingSetType.Workspace);
    public static final PendingSetType SHELVESET = new PendingSetType(_PendingSetType.Shelveset);

    private PendingSetType(final _PendingSetType pendingSetType) {
        super(pendingSetType);
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
    public static PendingSetType fromWebServiceObject(final _PendingSetType webServiceObject) {
        return (PendingSetType) EnumerationWrapper.fromWebServiceObject(webServiceObject);
    }

    /**
     * Gets the web service object this class wraps. The returned object should
     * not be modified.
     *
     * @return the web service object this class wraps.
     */
    public _PendingSetType getWebServiceObject() {
        return (_PendingSetType) webServiceObject;
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
