// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.flags;

import com.microsoft.tfs.core.internal.wrappers.EnumerationWrapper;

import ms.tfs.build.buildservice._03._BuildPhaseStatus;

/**
 * Describes the status of the build phase.
 *
 * @since TEE-SDK-10.1
 */
public class BuildPhaseStatus2010 extends EnumerationWrapper {
    public static final BuildPhaseStatus2010 UNKOWN = new BuildPhaseStatus2010(_BuildPhaseStatus.Unknown);
    public static final BuildPhaseStatus2010 FAILED = new BuildPhaseStatus2010(_BuildPhaseStatus.Failed);
    public static final BuildPhaseStatus2010 SUCCEEDED = new BuildPhaseStatus2010(_BuildPhaseStatus.Succeeded);

    private BuildPhaseStatus2010(final _BuildPhaseStatus buildPhaseStatus) {
        super(buildPhaseStatus);
    }

    /**
     * Gets the correct wrapper type for the given web service object.
     *
     * @param webServiceObject
     *        the web service object (must not be <code>null</code>)
     * @return the correct wrapper type for the given web service object
     * @throws RuntimeException
     *         if no wrapper type is known for the given web service object
     */
    public static BuildPhaseStatus2010 fromWebServiceObject(final _BuildPhaseStatus webServiceObject) {
        if (webServiceObject == null) {
            return null;
        }

        return (BuildPhaseStatus2010) EnumerationWrapper.fromWebServiceObject(webServiceObject);
    }

    /**
     * Gets the web service object this class wraps. The returned object should
     * not be modified.
     *
     * @return the web service object this class wraps.
     */
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
