// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.soapextensions;

import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;
import com.microsoft.tfs.core.internal.wrappers.WrapperUtils;

import ms.tfs.versioncontrol.clientservices._03._ReconcileResult;

/**
 * Wrapper object for the _ReconcileResult generated proxy.
 *
 * @since TEE-SDK-11.0
 */
public final class ReconcileResult extends WebServiceObjectWrapper {
    public ReconcileResult(final _ReconcileResult result) {
        super(result);
    }

    /**
     * Gets the web service object this class wraps. The returned object should
     * not be modified.
     *
     * @return the web service object this class wraps.
     */
    public _ReconcileResult getWebServiceObject() {
        return (_ReconcileResult) webServiceObject;
    }

    public synchronized Failure[] getFailures() {
        return (Failure[]) WrapperUtils.wrap(Failure.class, getWebServiceObject().getFailures());
    }

    public synchronized String getNewSignature() {
        return getWebServiceObject().getNewSignature();
    }

    public synchronized PendingChange[] getNewPendingChanges() {
        return (PendingChange[]) WrapperUtils.wrap(PendingChange.class, getWebServiceObject().getNewPendingChanges());
    }

    public synchronized boolean isPendingChangesUpdated() {
        return getWebServiceObject().isPendingChangesUpdated();
    }

    public synchronized boolean isReplayLocalVersionsRequired() {
        return getWebServiceObject().isReplayLocalVersionsRequired();
    }
}
