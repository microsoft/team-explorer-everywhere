// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.soapextensions;

import java.util.Calendar;

import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;
import com.microsoft.tfs.core.internal.wrappers.WrapperUtils;

import ms.tfs.versioncontrol.clientservices._03._BranchObject;

/**
 * Describes properties of a BranchObject class that are relevant to the
 * repository.
 *
 * @since TEE-SDK-10.1
 */
public class BranchObject extends WebServiceObjectWrapper {
    public BranchObject(final _BranchObject webServiceObject) {
        super(webServiceObject);
    }

    /**
     * Gets the web service object this class wraps. The returned object should
     * not be modified.
     *
     * @return the web service object this class wraps.
     */
    public _BranchObject getWebServiceObject() {
        return (_BranchObject) webServiceObject;
    }

    public Calendar getDateCreated() {
        return getWebServiceObject().getDateCreated();
    }

    public ItemIdentifier[] getChildBranches() {
        return (ItemIdentifier[]) WrapperUtils.wrap(ItemIdentifier.class, getWebServiceObject().getChildBranches());
    }

    public ItemIdentifier[] getRelatedBranches() {
        return (ItemIdentifier[]) WrapperUtils.wrap(ItemIdentifier.class, getWebServiceObject().getRelatedBranches());
    }

    public BranchProperties getProperties() {
        return new BranchProperties(getWebServiceObject().getProperties());
    }

    public boolean isDeleted() {
        final ItemIdentifier rootItem = getProperties().getRootItem();
        if (rootItem != null) {
            return rootItem.getDeletionID() > 0;
        }
        return false;
    }

    @Override
    public String toString() {
        return getProperties().getRootItem().getItem();
    }
}
