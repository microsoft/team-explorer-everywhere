// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.rules;

import com.microsoft.tfs.core.clients.workitem.internal.WorkItemImpl;

public interface RuleConstraint {
    public boolean passesConstraint(WorkItemImpl workItem);
}
