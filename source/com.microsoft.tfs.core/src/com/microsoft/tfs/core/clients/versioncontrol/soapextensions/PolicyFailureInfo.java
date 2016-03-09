// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.soapextensions;

import com.microsoft.tfs.core.checkinpolicies.PolicyFailure;
import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;

import ms.tfs.versioncontrol.clientservices._03._PolicyFailureInfo;

/**
 * Contains a checkin policy failure that's sent back to the server with an
 * excuse inside an {@link PolicyOverrideInfo} object.
 *
 * @since TEE-SDK-10.1
 */
public final class PolicyFailureInfo extends WebServiceObjectWrapper {
    public PolicyFailureInfo() {
        super(new _PolicyFailureInfo());
    }

    public PolicyFailureInfo(final _PolicyFailureInfo failure) {
        super(failure);
    }

    public PolicyFailureInfo(final String policyName, final String message) {
        super(new _PolicyFailureInfo(policyName, message));
    }

    public PolicyFailureInfo(final PolicyFailure policyFailure) {
        super(new _PolicyFailureInfo(policyFailure.getPolicy().getPolicyType().getName(), policyFailure.getMessage()));
    }

    /**
     * Gets the web service object this class wraps. The returned object should
     * not be modified.
     *
     * @return the web service object this class wraps.
     */
    public _PolicyFailureInfo getWebServiceObject() {
        return (_PolicyFailureInfo) webServiceObject;
    }

    /**
     * @return the failure message
     */
    public String getMessage() {
        return getWebServiceObject().getMessage();
    }
}
