// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.flags;

import com.microsoft.tfs.core.internal.wrappers.EnumerationWrapper;

import ms.tfs.build.buildservice._03._AgentStatus;

/**
 * Describes the status of a build agent.
 *
 * @since TEE-SDK-10.1
 */
public class AgentStatus2010 extends EnumerationWrapper {
    public static final AgentStatus2010 UNAVAILABLE = new AgentStatus2010(_AgentStatus.Unavailable);
    public static final AgentStatus2010 AVAILABLE = new AgentStatus2010(_AgentStatus.Available);
    public static final AgentStatus2010 OFFLINE = new AgentStatus2010(_AgentStatus.Offline);

    private AgentStatus2010(final _AgentStatus type) {
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
    public static AgentStatus2010 fromWebServiceObject(final _AgentStatus webServiceObject) {
        if (webServiceObject == null) {
            return null;
        }
        return (AgentStatus2010) EnumerationWrapper.fromWebServiceObject(webServiceObject);
    }

    /**
     * Gets the web service object this class wraps. The returned object should
     * not be modified.
     *
     * @return the web service object this class wraps.
     */
    public _AgentStatus getWebServiceObject() {
        return (_AgentStatus) webServiceObject;
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
