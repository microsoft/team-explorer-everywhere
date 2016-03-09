// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build;

public interface IBuildQueryResult {
    /**
     * The array of builds for this query result.
     *
     *
     * @return
     */
    public IBuildDetail[] getBuilds();

    /**
     * The array of failures for this query result.
     *
     *
     * @return
     */
    public IFailure[] getFailures();
}
