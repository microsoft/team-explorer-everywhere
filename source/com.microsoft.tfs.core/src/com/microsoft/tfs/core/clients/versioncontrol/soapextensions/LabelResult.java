// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.soapextensions;

import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;

import ms.tfs.versioncontrol.clientservices._03._LabelResult;

/**
 * The result of a label creation operation.
 *
 * @since TEE-SDK-10.1
 */
public final class LabelResult extends WebServiceObjectWrapper {
    public LabelResult(final _LabelResult result) {
        super(result);
    }

    /**
     * Gets the web service object this class wraps. The returned object should
     * not be modified.
     *
     * @return the web service object this class wraps.
     */
    public _LabelResult getWebServiceObject() {
        return (_LabelResult) webServiceObject;
    }

    public synchronized String getLabel() {
        return getWebServiceObject().getLabel();
    }

    public synchronized LabelResultStatus getStatus() {
        return LabelResultStatus.fromWebServiceObject(getWebServiceObject().getStatus());
    }

    public synchronized String getScope() {
        return getWebServiceObject().getScope();
    }
}
