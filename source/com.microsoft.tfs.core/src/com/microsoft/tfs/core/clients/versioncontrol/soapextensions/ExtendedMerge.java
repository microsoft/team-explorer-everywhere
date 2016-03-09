// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.soapextensions;

import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;

import ms.tfs.versioncontrol.clientservices._03._ExtendedMerge;

/**
 * Represents extended information about a merge.
 *
 * @since TEE-SDK-10.1
 */
public final class ExtendedMerge extends WebServiceObjectWrapper {
    public ExtendedMerge() {
        super(new _ExtendedMerge());
    }

    public ExtendedMerge(final _ExtendedMerge merge) {
        super(merge);
    }

    public ExtendedMerge(
        final ChangesetSummary sourceChangeset,
        final ChangesetSummary targetChangeset,
        final int versionedItemCount,
        final Change sourceItem,
        final ItemIdentifier targetItem) {

        super(
            new _ExtendedMerge(
                sourceChangeset.getWebServiceObject(),
                targetChangeset.getWebServiceObject(),
                versionedItemCount,
                sourceItem.getWebServiceObject(),
                targetItem.getWebServiceObject()));
    }

    /**
     * Gets the web service object this class wraps. The returned object should
     * not be modified.
     *
     * @return the web service object this class wraps.
     */
    public _ExtendedMerge getWebServiceObject() {
        return (_ExtendedMerge) webServiceObject;
    }

    public ChangesetSummary getSourceChangeset() {
        return new ChangesetSummary(getWebServiceObject().getSourceChangeset());
    }

    public ChangesetSummary getTargetChangeset() {
        return new ChangesetSummary(getWebServiceObject().getTargetChangeset());
    }

    public Change getSourceItem() {
        return new Change(getWebServiceObject().getSourceItem());
    }

    public ItemIdentifier getTargetItem() {
        return new ItemIdentifier(getWebServiceObject().getTargetItem());
    }

    public int getVersionedItemCount() {
        return getWebServiceObject().getVersionedItemCount();
    }
}
