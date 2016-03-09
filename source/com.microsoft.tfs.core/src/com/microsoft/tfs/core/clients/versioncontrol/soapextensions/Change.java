// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.soapextensions;

import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;
import com.microsoft.tfs.core.internal.wrappers.WrapperUtils;

import ms.tfs.versioncontrol.clientservices._03._Change;
import ms.tfs.versioncontrol.clientservices._03._MergeSource;

/**
 * Contains information about a modification made to a version control item
 * (file or folder) as part of a {@link Changeset}.
 *
 * @since TEE-SDK-10.1
 */
public final class Change extends WebServiceObjectWrapper implements Comparable<Change> {
    public Change() {
        super(new _Change());
    }

    public Change(final _Change change) {
        super(change);
    }

    public Change(final Item item, final ChangeType type, final MergeSource[] mergeSources) {
        super(
            new _Change(
                type.getWebServiceObject(),
                type.getWebServiceObjectExtendedFlags(),
                item.getWebServiceObject(),
                (_MergeSource[]) WrapperUtils.unwrap(_MergeSource.class, mergeSources)));
    }

    /**
     * Gets the web service object this class wraps. The returned object should
     * not be modified.
     *
     * @return the web service object this class wraps.
     */
    public _Change getWebServiceObject() {
        return (_Change) webServiceObject;
    }

    public Item getItem() {
        return new Item(getWebServiceObject().getItem());
    }

    public ChangeType getChangeType() {
        return new ChangeType(getWebServiceObject().getType(), getWebServiceObject().getTypeEx());
    }

    public void setChangeType(final ChangeType type) {
        getWebServiceObject().setType(type.getWebServiceObject());
        getWebServiceObject().setTypeEx(type.getWebServiceObjectExtendedFlags());
    }

    public MergeSource[] getMergeSources() {
        return (MergeSource[]) WrapperUtils.wrap(MergeSource.class, getWebServiceObject().getMergeSources());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int compareTo(final Change o) {
        return getItem().compareTo(o.getItem());
    }
}
