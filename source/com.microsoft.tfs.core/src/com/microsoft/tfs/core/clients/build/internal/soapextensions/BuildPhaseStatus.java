// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.internal.soapextensions;

import com.microsoft.tfs.core.internal.wrappers.EnumerationWrapper;

import ms.tfs.build.buildservice._04._BuildPhaseStatus;

public class BuildPhaseStatus extends EnumerationWrapper {
    public static final BuildPhaseStatus UNKOWN = new BuildPhaseStatus(_BuildPhaseStatus.Unknown);
    public static final BuildPhaseStatus FAILED = new BuildPhaseStatus(_BuildPhaseStatus.Failed);
    public static final BuildPhaseStatus SUCCEEDED = new BuildPhaseStatus(_BuildPhaseStatus.Succeeded);

    private BuildPhaseStatus(final _BuildPhaseStatus buildPhaseStatus) {
        super(buildPhaseStatus);
    }

    public static BuildPhaseStatus fromWebServiceObject(final _BuildPhaseStatus webServiceObject) {
        if (webServiceObject == null) {
            return null;
        }

        return (BuildPhaseStatus) EnumerationWrapper.fromWebServiceObject(webServiceObject);
    }

    public _BuildPhaseStatus getWebServiceObject() {
        return (_BuildPhaseStatus) webServiceObject;
    }

    @Override
    public boolean equals(final Object o) {
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
