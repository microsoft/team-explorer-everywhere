// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build;

public interface IBuildAgentQueryResult {
    /**
     * The array of build agents for this query result.
     *
     *
     * @return
     */
    public IBuildAgent[] getAgents();

    /**
     * The array of failures for this query result.
     *
     *
     * @return
     */
    public IFailure[] getFailures();
}
