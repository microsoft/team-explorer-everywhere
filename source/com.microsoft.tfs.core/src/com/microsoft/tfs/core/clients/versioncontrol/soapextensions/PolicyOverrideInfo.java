// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.soapextensions;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.core.checkinpolicies.PolicyFailure;
import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;
import com.microsoft.tfs.core.internal.wrappers.WrapperUtils;
import com.microsoft.tfs.util.Check;

import ms.tfs.versioncontrol.clientservices._03._PolicyFailureInfo;
import ms.tfs.versioncontrol.clientservices._03._PolicyOverrideInfo;

/**
 * Contain's a user's excuse for why they overrode a checkin policy failure.
 *
 * @since TEE-SDK-10.1
 */
public final class PolicyOverrideInfo extends WebServiceObjectWrapper {
    public PolicyOverrideInfo() {
        super(new _PolicyOverrideInfo());
    }

    public PolicyOverrideInfo(final _PolicyOverrideInfo info) {
        super(info);
    }

    public PolicyOverrideInfo(final String comment, final PolicyFailureInfo[] failureInfos) {
        super(new _PolicyOverrideInfo(comment, getWebServiceObjectInfoArray(failureInfos)));
    }

    public PolicyOverrideInfo(final String comment, final PolicyFailure[] failures) {
        super(new _PolicyOverrideInfo(comment, getWebServiceObjectFailureArray(failures)));
    }

    private static _PolicyFailureInfo[] getWebServiceObjectInfoArray(final PolicyFailureInfo[] infos) {
        Check.notNull(infos, "infos"); //$NON-NLS-1$

        return (_PolicyFailureInfo[]) WrapperUtils.unwrap(_PolicyFailureInfo.class, infos);
    }

    private static _PolicyFailureInfo[] getWebServiceObjectFailureArray(final PolicyFailure[] failures) {
        Check.notNull(failures, "infos"); //$NON-NLS-1$

        final _PolicyFailureInfo[] ret = new _PolicyFailureInfo[failures.length];
        for (int i = 0; i < failures.length; i++) {
            final PolicyFailure failure = failures[i];

            final String policyName = (failure.getPolicy() != null) ? failure.getPolicy().getPolicyType().getName()
                : "[" + Messages.getString("PolicyOverrideInfo.InvalidPolicy") + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

            ret[i] = new _PolicyFailureInfo(policyName, failure.getMessage());
        }
        return ret;
    }

    /**
     * Gets the web service object this class wraps. The returned object should
     * not be modified.
     *
     * @return the web service object this class wraps.
     */
    public _PolicyOverrideInfo getWebServiceObject() {
        return (_PolicyOverrideInfo) webServiceObject;
    }

    public String getComment() {
        return getWebServiceObject().getComment();
    }

    public PolicyFailureInfo[] getPolicyFailures() {
        final _PolicyFailureInfo[] internalFailures = getWebServiceObject().getPolicyFailures();

        if (internalFailures == null) {
            return new PolicyFailureInfo[0];
        }

        return (PolicyFailureInfo[]) WrapperUtils.wrap(PolicyFailureInfo.class, internalFailures);
    }
}
