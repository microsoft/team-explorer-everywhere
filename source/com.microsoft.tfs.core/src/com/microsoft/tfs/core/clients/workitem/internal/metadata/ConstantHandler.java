// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.metadata;

public interface ConstantHandler {
    public IConstantSet getConstantSet(
        int[] rootConstantIDs,
        boolean oneLevel,
        boolean twoPlusLevels,
        boolean leaf,
        boolean interior,
        boolean useCache);

    public IConstantSet getConstantSet(
        int rootConstantID,
        boolean oneLevel,
        boolean twoPlusLevels,
        boolean leaf,
        boolean interior,
        boolean useCache);
}
