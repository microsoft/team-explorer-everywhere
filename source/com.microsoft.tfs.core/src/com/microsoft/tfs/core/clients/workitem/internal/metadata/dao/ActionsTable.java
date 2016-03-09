// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.metadata.dao;

public interface ActionsTable {
    public String getNextStateForAction(String currentState, String action, int workItemTypeID);
}
