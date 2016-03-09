// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.soapextensions;

import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;

import ms.tfs.versioncontrol.clientservices._03._ChangesetMerge;

/**
 * Represents a merge of one {@link Changeset} into another {@link Changeset}.
 *
 * @since TEE-SDK-10.1
 */
public class ChangesetMerge extends WebServiceObjectWrapper {
    /**
     * The full information about the changeset this merge was for, set via
     * {@link #setTargetChangeset(Changeset)}.
     */
    private Changeset targetChangeset;

    public ChangesetMerge() {
        super(new _ChangesetMerge());
    }

    public ChangesetMerge(final _ChangesetMerge changesetMerge) {
        this(changesetMerge.getSrcver(), changesetMerge.getTgtver(), changesetMerge.isPart());
    }

    public ChangesetMerge(final int sourceVersion, final int targetVersion, final boolean isPartial) {
        super(new _ChangesetMerge(sourceVersion, targetVersion, isPartial));
    }

    /**
     * Gets the web service object this class wraps. The returned object should
     * not be modified.
     *
     * @return the web service object this class wraps.
     */
    public _ChangesetMerge getWebServiceObject() {
        return (_ChangesetMerge) webServiceObject;
    }

    public int getSourceVersion() {
        return getWebServiceObject().getSrcver();
    }

    public int getTargetVersion() {
        return getWebServiceObject().getTgtver();
    }

    public boolean isPartial() {
        return getWebServiceObject().isPart();
    }

    public void setPartial(final boolean part) {
        getWebServiceObject().setPart(part);
    }

    public void setSourceVersion(final int srcver) {
        getWebServiceObject().setSrcver(srcver);
    }

    public void setTargetVersion(final int tgtver) {
        getWebServiceObject().setTgtver(tgtver);
    }

    /**
     * @return the target changeset associated with this merge (may be null).
     */
    public Changeset getTargetChangeset() {
        return targetChangeset;
    }

    /**
     * @param targetChangeset
     *        the changeset associated with this merge (may be null).
     */
    public void setTargetChangeset(final Changeset targetChangeset) {
        this.targetChangeset = targetChangeset;
    }
}
