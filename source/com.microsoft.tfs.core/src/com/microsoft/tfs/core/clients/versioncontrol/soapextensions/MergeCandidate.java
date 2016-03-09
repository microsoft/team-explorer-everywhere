// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.soapextensions;

import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;

import ms.tfs.versioncontrol.clientservices._03._MergeCandidate;

/**
 * Wrapper around a merge canidate object returned by the web service.
 *
 * @since TEE-SDK-10.1
 */
public class MergeCandidate extends WebServiceObjectWrapper {
    public MergeCandidate() {
        super(new _MergeCandidate());
    }

    public MergeCandidate(final _MergeCandidate mergeCandidate) {
        super(mergeCandidate);
    }

    /**
     * Gets the web service object this class wraps. The returned object should
     * not be modified.
     *
     * @return the web service object this class wraps.
     */
    public _MergeCandidate getWebServiceObject() {
        return (_MergeCandidate) webServiceObject;
    }

    public Changeset getChangeset() {
        return new Changeset(getWebServiceObject().getChangeset());
    }

    public void setChangeset(final Changeset changeset) {
        getWebServiceObject().setChangeset(changeset.getWebServiceObject());
    }

    public boolean isPartial() {
        return getWebServiceObject().isPart();
    }

    public void setPartial(final boolean isPartial) {
        getWebServiceObject().setPart(isPartial);
    }
}
