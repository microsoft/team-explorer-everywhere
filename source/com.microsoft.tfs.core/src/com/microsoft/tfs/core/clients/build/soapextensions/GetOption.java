// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.soapextensions;

import com.microsoft.tfs.core.internal.wrappers.EnumerationWrapper;

import ms.tfs.build.buildservice._04._GetOption;

/**
 * Describes options for a get operation.
 *
 * @since TEE-SDK-10.1
 */
public class GetOption extends EnumerationWrapper {
    public static final GetOption LATEST_ON_QUEUE = new GetOption(_GetOption.LatestOnQueue);
    public static final GetOption LATEST_ON_BUILD = new GetOption(_GetOption.LatestOnBuild);
    public static final GetOption CUSTOM = new GetOption(_GetOption.Custom);

    private GetOption(final _GetOption getOption) {
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
    public static GetOption fromWebServiceObject(final _GetOption webServiceObject) {
        if (webServiceObject == null) {
            return null;
        }
        return (GetOption) EnumerationWrapper.fromWebServiceObject(webServiceObject);
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
