// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.flags;

import com.microsoft.tfs.core.internal.wrappers.EnumerationWrapper;

import ms.tfs.build.buildservice._03._BuildQueryOrder;

public class BuildQueryOrder2010 extends EnumerationWrapper {
    public static BuildQueryOrder2010 FINISH_TIME_ASCENDING =
        new BuildQueryOrder2010(_BuildQueryOrder.FinishTimeAscending);
    public static BuildQueryOrder2010 FINISH_TIME_DESCENDING =
        new BuildQueryOrder2010(_BuildQueryOrder.FinishTimeDescending);
    public static BuildQueryOrder2010 START_TIME_ASCENDING =
        new BuildQueryOrder2010(_BuildQueryOrder.StartTimeAscending);
    public static BuildQueryOrder2010 START_TIME_DESCENDING =
        new BuildQueryOrder2010(_BuildQueryOrder.StartTimeDescending);

    private BuildQueryOrder2010(final _BuildQueryOrder type) {
        super(type);
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
    public static BuildQueryOrder2010 fromWebServiceObject(final _BuildQueryOrder webServiceObject) {
        if (webServiceObject == null) {
            return null;
        }
        return (BuildQueryOrder2010) EnumerationWrapper.fromWebServiceObject(webServiceObject);
    }

    /**
     * Gets the web service object this class wraps. The returned object should
     * not be modified.
     *
     * @return the web service object this class wraps.
     */
    public _BuildQueryOrder getWebServiceObject() {
        return (_BuildQueryOrder) webServiceObject;
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
