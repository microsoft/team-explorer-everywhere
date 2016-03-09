// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.metadata.dao;

import com.microsoft.tfs.core.clients.workitem.internal.rules.Rule;

public interface RulesTable {
    public int getWorkItemFormID(int teamProjectId, String workItemTypeName);

    public Rule[] getRulesForAreaNode(int areaId);

    public Rule[] getRulesForThenFieldID(int fieldId);
}