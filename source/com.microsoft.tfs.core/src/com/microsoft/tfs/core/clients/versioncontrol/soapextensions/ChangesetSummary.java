// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.soapextensions;

import java.util.Calendar;

import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;

import ms.tfs.versioncontrol.clientservices._03._ChangesetSummary;

/**
 * Represents additional information about a {@link Changeset}.
 *
 * @since TEE-SDK-10.1
 */
public final class ChangesetSummary extends WebServiceObjectWrapper {
    public ChangesetSummary() {
        super(new _ChangesetSummary());
    }

    public ChangesetSummary(final _ChangesetSummary summary) {
        super(summary);

        final String displayName = summary.getOwnerDisplayName();
        if (displayName == null || displayName.length() == 0) {
            summary.setOwnerDisplayName(summary.getOwner());
            summary.setCommitterDisplayName(summary.getCommitter());
        }
    }

    public ChangesetSummary(
        final int changesetID,
        final String owner,
        final String ownerDisplayName,
        final String committer,
        final String committerDisplayName,
        final String comment,
        final Calendar creationDate) {
        super(
            new _ChangesetSummary(
                changesetID,
                owner,
                ownerDisplayName,
                committer,
                committerDisplayName,
                comment,
                creationDate));
    }

    /**
     * Gets the web service object this class wraps. The returned object should
     * not be modified.
     *
     * @return the web service object this class wraps.
     */
    public _ChangesetSummary getWebServiceObject() {
        return (_ChangesetSummary) webServiceObject;
    }

    public int getChangesetID() {
        return getWebServiceObject().getChangesetId();
    }

    public String getOwner() {
        return getWebServiceObject().getOwner();
    }

    public String getOwnerDisplayName() {
        return getWebServiceObject().getOwnerDisplayName();
    }

    public String getCommitter() {
        return getWebServiceObject().getCommitter();
    }

    public String getCommitterDisplayName() {
        return getWebServiceObject().getCommitterDisplayName();
    }

    public String getComment() {
        return getWebServiceObject().getComment();
    }

    public Calendar getCreationDate() {
        return getWebServiceObject().getCreationDate();
    }
}
