// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build;

public interface IBuildControllerQueryResult {
    /**
     * The array of build controllers for this query result.
     *
     *
     * @return
     */
    public IBuildController[] getControllers();

    /**
     * The array of failures for this query result.
     *
     *
     * @return
     */
    public IFailure[] getFailures();
}
