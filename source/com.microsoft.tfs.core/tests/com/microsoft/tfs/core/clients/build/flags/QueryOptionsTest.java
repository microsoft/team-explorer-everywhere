// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.flags;

import junit.framework.TestCase;
import ms.tfs.build.buildservice._04._QueryOptions;
import ms.tfs.build.buildservice._04._QueryOptions._QueryOptions_Flag;

public class QueryOptionsTest extends TestCase {

    public void testGetWebServiceObject() {
        _QueryOptions_Flag[] flags = QueryOptions.PROCESS.getWebServiceObject().getFlags();
        assertEquals(1, flags.length);
        assertEquals(_QueryOptions_Flag.Process, flags[0]);

        flags = QueryOptions.ALL.getWebServiceObject().getFlags();
        assertEquals(1, flags.length);
        assertEquals(_QueryOptions_Flag.All, flags[0]);

        flags = QueryOptions.NONE.getWebServiceObject().getFlags();
        assertEquals(1, flags.length);
        assertEquals(_QueryOptions_Flag.None, flags[0]);

        flags = QueryOptions.ALL.remove(QueryOptions.PROCESS).remove(QueryOptions.CONTROLLERS).remove(
            QueryOptions.WORKSPACES).remove(QueryOptions.AGENTS).remove(QueryOptions.DEFINITIONS).remove(
                QueryOptions.BATCHED_REQUESTS).remove(QueryOptions.HISTORICAL_BUILDS).getWebServiceObject().getFlags();
        assertEquals(1, flags.length);
        assertEquals(_QueryOptions_Flag.None, flags[0]);

    }

    public void testFromWebServiceObject() {
        assertEquals(QueryOptions.CONTROLLERS, QueryOptions.fromWebServiceObject((new _QueryOptions(new String[] {
            "Controllers" //$NON-NLS-1$
        }))));
        assertEquals(QueryOptions.PROCESS, QueryOptions.fromWebServiceObject((new _QueryOptions(new String[] {
            "Process" //$NON-NLS-1$
        }))));
        assertEquals(QueryOptions.PROCESS, QueryOptions.fromWebServiceObject((new _QueryOptions(new String[] {
            "Process", //$NON-NLS-1$
            "Definitions" //$NON-NLS-1$
        }))));
        assertFalse(QueryOptions.CONTROLLERS.equals(QueryOptions.fromWebServiceObject((new _QueryOptions(new String[] {
            "Controllers", //$NON-NLS-1$
            "Definitions" //$NON-NLS-1$
        })))));

    }

    public void testContainsAll() {
        final QueryOptions expected = QueryOptions.fromWebServiceObject((new _QueryOptions(new String[] {
            "Controllers", //$NON-NLS-1$
            "Definitions" //$NON-NLS-1$
        })));

        final QueryOptions actual = QueryOptions.CONTROLLERS.combine(QueryOptions.DEFINITIONS);

        assertTrue(actual.containsAll(expected));

        assertTrue(
            QueryOptions.ALL.containsAll(
                QueryOptions.NONE.combine(QueryOptions.DEFINITIONS).combine(QueryOptions.AGENTS).combine(
                    QueryOptions.WORKSPACES).combine(QueryOptions.CONTROLLERS).combine(QueryOptions.PROCESS)));

    }

    public void testContains() {
        assertTrue(QueryOptions.ALL.contains(QueryOptions.CONTROLLERS));
        assertFalse(QueryOptions.NONE.contains(QueryOptions.DEFINITIONS));

        // The "funny" ones
        assertTrue(QueryOptions.PROCESS.contains(QueryOptions.DEFINITIONS));
        assertTrue(QueryOptions.WORKSPACES.contains(QueryOptions.DEFINITIONS));
        assertFalse(QueryOptions.CONTROLLERS.contains(QueryOptions.DEFINITIONS));
        assertFalse(QueryOptions.AGENTS.contains(QueryOptions.DEFINITIONS));

        assertFalse(QueryOptions.DEFINITIONS.contains(QueryOptions.PROCESS));
    }

    public void testRemove() {
        final QueryOptions options = QueryOptions.ALL.remove(QueryOptions.AGENTS);
        final _QueryOptions_Flag[] flags = options.getWebServiceObject().getFlags();
        assertEquals(6, flags.length);

        final String expected = "Definitions,Workspaces,Controllers,Process,BatchedRequests,HistoricalBuilds"; //$NON-NLS-1$
        for (int i = 0; i < flags.length; i++) {
            assertTrue("Flag: \"" + flags[i].toString() + "\" not found", expected.indexOf(flags[i].toString()) >= 0); //$NON-NLS-1$ //$NON-NLS-2$
        }
    }

}
