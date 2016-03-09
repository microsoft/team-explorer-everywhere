// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.soapextensions;

import com.microsoft.tfs.core.clients.versioncontrol.path.LocalPath;
import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;

import ms.tfs.versioncontrol.clientservices._03._ConflictInformation;

/**
 * @since TEE-SDK-11.0
 */
public final class ConflictInformation extends WebServiceObjectWrapper {
    public ConflictInformation(final _ConflictInformation conflictInfo) {
        super(conflictInfo);
    }

    /**
     * Gets the web service object this class wraps. The returned object should
     * not be modified.
     *
     * @return the web service object this class wraps.
     */
    public _ConflictInformation getWebServiceObject() {
        return (_ConflictInformation) webServiceObject;
    }

    /**
     *
     * @return the type of conflict represented.
     */
    public ConflictType getConflictType() {
        return ConflictType.fromWebServiceObject(getWebServiceObject().getCt());
    }

    /**
     * @return the ID of the pending change this item is in conflict with
     */
    public int getPendingChangeID() {
        return getWebServiceObject().getPcid();
    }

    // TODO what is this? Can we define an enumeration/flags type?
    /**
     * @return the reason for the conflict. This is only used by Local
     *         conflicts.
     */
    public int getReason() {
        return getWebServiceObject().getRe();
    }

    /**
     *
     * @return where the item is currently on the local machine
     */
    public String getSourceLocalItem() {
        return LocalPath.tfsToNative(getWebServiceObject().getSlocal());

    }

    /**
     *
     * @return where the item is supposed to be on the local machine.
     */
    public String getTargetLocalItem() {
        return LocalPath.tfsToNative(getWebServiceObject().getTlocal());

    }

    /**
     * @return the version that this item is in conflict with
     */
    public int getVersionFrom() {
        return getWebServiceObject().getVf();
    }
}
