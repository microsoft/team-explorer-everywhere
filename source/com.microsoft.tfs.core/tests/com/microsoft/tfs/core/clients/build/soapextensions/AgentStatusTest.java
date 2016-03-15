// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.soapextensions;

import junit.framework.TestCase;

public class AgentStatusTest extends TestCase {

    public void testGetAgentStatus() {
        AgentStatus.AVAILABLE.getWebServiceObject();
        assertEquals("Available", AgentStatus.AVAILABLE.toString()); //$NON-NLS-1$

        AgentStatus.UNAVAILABLE.getWebServiceObject();
        assertEquals("Unavailable", AgentStatus.UNAVAILABLE.toString()); //$NON-NLS-1$

        AgentStatus.OFFLINE.getWebServiceObject();
        assertEquals("Offline", AgentStatus.OFFLINE.toString()); //$NON-NLS-1$
    }

}
