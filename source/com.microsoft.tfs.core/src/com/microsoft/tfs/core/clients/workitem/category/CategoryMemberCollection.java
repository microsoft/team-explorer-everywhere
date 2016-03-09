// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.category;

import com.microsoft.tfs.core.clients.workitem.wittype.WorkItemType;

public interface CategoryMemberCollection {
    WorkItemType[] getCategoryMembers(final int categoryID);
}
