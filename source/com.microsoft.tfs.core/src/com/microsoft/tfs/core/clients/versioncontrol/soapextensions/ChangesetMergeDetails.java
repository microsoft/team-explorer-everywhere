// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.soapextensions;

import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;

import ms.tfs.versioncontrol.clientservices._03._ChangesetMergeDetails;

/**
 * Contains detailed information about a {@link Changeset} that was merged into
 * another branch.
 *
 * @since TEE-SDK-10.1
 */
public class ChangesetMergeDetails extends WebServiceObjectWrapper {
    public ChangesetMergeDetails() {
        super(new _ChangesetMergeDetails());
    }

    public ChangesetMergeDetails(final _ChangesetMergeDetails details) {
        super(details);
    }

    /**
     * Gets the web service object this class wraps. The returned object should
     * not be modified.
     *
     * @return the web service object this class wraps.
     */
    public _ChangesetMergeDetails getWebServiceObject() {
        return (_ChangesetMergeDetails) webServiceObject;
    }

    public Changeset[] getChangesets() {
        final Changeset[] ret = new Changeset[getWebServiceObject().getChangesets().length];

        for (int i = 0; i < ret.length; i++) {
            ret[i] = new Changeset(getWebServiceObject().getChangesets()[i]);
        }

        return ret;
    }

    public ItemMerge[] getMergedItems() {
        final ItemMerge[] ret = new ItemMerge[getWebServiceObject().getMergedItems().length];

        for (int i = 0; i < ret.length; i++) {
            ret[i] = new ItemMerge(getWebServiceObject().getMergedItems()[i]);
        }

        return ret;
    }

    public ItemMerge[] getUnmergedItems() {
        final ItemMerge[] ret = new ItemMerge[getWebServiceObject().getUnmergedItems().length];

        for (int i = 0; i < ret.length; i++) {
            ret[i] = new ItemMerge(getWebServiceObject().getUnmergedItems()[i]);
        }

        return ret;
    }
}
