// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.soapextensions;

import com.microsoft.tfs.core.internal.wrappers.EnumerationWrapper;

import ms.tfs.versioncontrol.clientservices._03._DeletedState;

/**
 * Describes the deleted state of an item.
 *
 * @since TEE-SDK-10.1
 */
public class DeletedState extends EnumerationWrapper {
    public static final DeletedState NON_DELETED = new DeletedState(_DeletedState.NonDeleted);
    public static final DeletedState DELETED = new DeletedState(_DeletedState.Deleted);
    public static final DeletedState ANY = new DeletedState(_DeletedState.Any);

    private DeletedState(final _DeletedState state) {
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
    public static DeletedState fromWebServiceObject(final _DeletedState webServiceObject) {
        return (DeletedState) EnumerationWrapper.fromWebServiceObject(webServiceObject);
    }

    /**
     * Gets the web service object this class wraps. The returned object should
     * not be modified.
     *
     * @return the web service object this class wraps.
     */
    public _DeletedState getWebServiceObject() {
        return (_DeletedState) webServiceObject;
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
