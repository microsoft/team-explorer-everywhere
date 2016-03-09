// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build;

public interface IBuildDefinitionQueryResult {
    /**
     * The array of build definitions for this query result.
     *
     *
     * @return
     */
    public IBuildDefinition[] getDefinitions();

    /**
     * The array of failures for this query result.
     *
     *
     * @return
     */
    public IFailure[] getFailures();
}
