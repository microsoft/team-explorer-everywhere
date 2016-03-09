// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build;

public interface IQueuedBuildQueryResult {
    /**
     * The array of queued builds for this query result.
     *
     *
     * @return
     */
    public IQueuedBuild[] getQueuedBuilds();

    /**
     * The array of failures for this query result.
     *
     *
     * @return
     */
    public IFailure[] getFailures();
}
