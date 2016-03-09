// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.soapextensions;

import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;

import ms.tfs.versioncontrol.clientservices._03._BranchObjectOwnership;

/**
 * A summary class used to return branch root information from the server
 *
 * @since TEE-SDK-11.0
 */
public class BranchObjectOwnership extends WebServiceObjectWrapper {
    public BranchObjectOwnership(final _BranchObjectOwnership webServiceObject) {
        super(webServiceObject);
    }

    /**
     * Gets the web service object this class wraps. The returned object should
     * not be modified.
     *
     * @return the web service object this class wraps.
     */
    public _BranchObjectOwnership getWebServiceObject() {
        return (_BranchObjectOwnership) webServiceObject;
    }

    /**
     * @return the root item of the branch object.
     */
    public ItemIdentifier getRootItem() {
        return new ItemIdentifier(getWebServiceObject().getRootItem());
    }

    /**
     *
     * @return the count of items at different versions e.g. if an item exists
     *         at 2 versions being tracked it will be counted twice
     */
    public int getVersionedItemCount() {
        return getWebServiceObject().getVersionedItemCount();
    }
}
