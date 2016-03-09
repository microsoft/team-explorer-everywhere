// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.soapextensions;

import com.microsoft.tfs.core.internal.wrappers.EnumerationWrapper;

import ms.tfs.build.buildservice._03._Agent2008Status;

/**
 * Describes the status of a 2008 build agent.
 *
 * @since TEE-SDK-10.1
 */
public class Agent2008Status extends EnumerationWrapper {
    public static final Agent2008Status ENABLED = new Agent2008Status(_Agent2008Status.Enabled);
    public static final Agent2008Status DISABLED = new Agent2008Status(_Agent2008Status.Disabled);
    public static final Agent2008Status UNREACHABLE = new Agent2008Status(_Agent2008Status.Unreachable);
    public static final Agent2008Status INITIALIZING = new Agent2008Status(_Agent2008Status.Initializing);

    private Agent2008Status(final _Agent2008Status agentStatus) {
        super(agentStatus);
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
    public static Agent2008Status fromWebServiceObject(final _Agent2008Status webServiceObject) {
        if (webServiceObject == null) {
            return null;
        }
        return (Agent2008Status) EnumerationWrapper.fromWebServiceObject(webServiceObject);
    }

    /**
     * Gets the web service object this class wraps. The returned object should
     * not be modified.
     *
     * @return the web service object this class wraps.
     */
    public _Agent2008Status getWebServiceObject() {
        return (_Agent2008Status) webServiceObject;
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
