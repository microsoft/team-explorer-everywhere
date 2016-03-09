// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.soapextensions;

import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;

import ms.tfs.versioncontrol.clientservices._03._PendingState;

/**
 * @since TEE-SDK-11.0
 */
public final class PendingState extends WebServiceObjectWrapper {
    public PendingState(final _PendingState pendingState) {
        super(pendingState);
    }

    /**
     * Gets the web service object this class wraps. The returned object should
     * not be modified.
     *
     * @return the web service object this class wraps.
     */
    public _PendingState getWebServiceObject() {
        return (_PendingState) webServiceObject;
    }

    /**
     * @return Information about any conflict that may exist for this pending
     *         change. If this is specified a conflict is registered for this
     *         item.
     */
    public ConflictInformation getConflictInfo() {
        return new ConflictInformation(getWebServiceObject().getConflictInfo());
    }

    /**
     * @return the item ID for the item containing the conflict
     */
    public int getItemID() {
        return getWebServiceObject().getId();
    }

    /**
     * @return the revert to version is specified to tell the server that it
     *         could not retrieve a file do to local changes and that if the
     *         changes are undone, this is the version that should be on the
     *         machine.
     */
    public int getRevertToVersion() {
        return getWebServiceObject().getRtv();

    }
}
