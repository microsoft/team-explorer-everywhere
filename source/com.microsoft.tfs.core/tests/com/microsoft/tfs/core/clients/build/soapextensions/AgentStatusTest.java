// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.soapextensions;

import junit.framework.TestCase;

public class AgentStatusTest extends TestCase {

    public void testGetAgentStatus() {
        Agent2008Status.ENABLED.getWebServiceObject();
        assertEquals("Enabled", Agent2008Status.ENABLED.toString()); //$NON-NLS-1$
    }

}
