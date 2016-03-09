// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.flags;

import com.microsoft.tfs.core.internal.wrappers.EnumerationWrapper;

import ms.tfs.build.buildservice._03._GetOption;

/**
 * Describes options for a get operation.
 *
 * @since TEE-SDK-10.1
 */
public class GetOption2010 extends EnumerationWrapper {
    public static final GetOption2010 LATEST_ON_QUEUE = new GetOption2010(_GetOption.LatestOnQueue);
    public static final GetOption2010 LATEST_ON_BUILD = new GetOption2010(_GetOption.LatestOnBuild);
    public static final GetOption2010 CUSTOM = new GetOption2010(_GetOption.Custom);

    private GetOption2010(final _GetOption getOption) {
        super(getOption);
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
    public static GetOption2010 fromWebServiceObject(final _GetOption webServiceObject) {
        if (webServiceObject == null) {
            return null;
        }
        return (GetOption2010) EnumerationWrapper.fromWebServiceObject(webServiceObject);
    }

    /**
     * Gets the web service object this class wraps. The returned object should
     * not be modified.
     *
     * @return the web service object this class wraps.
     */
    public _GetOption getWebServiceObject() {
        return (_GetOption) webServiceObject;
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
