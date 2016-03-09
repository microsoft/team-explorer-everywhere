// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.metadata.impl;

import com.microsoft.tfs.core.clients.workitem.internal.metadata.dao.WorkItemTypeUsagesTable;

public class WorkItemTypeUsagesTableImpl extends BaseMetadataDAO implements WorkItemTypeUsagesTable {
    @Override
    public int[] getFieldIDsForWorkItemType(final int workItemTypeId) {
        return (int[]) getConnection().createStatement(
            "select FieldID from WorkItemTypeUsages where WorkItemTypeID = ?").executeQueryForPrimitiveArray( //$NON-NLS-1$
                new Integer(workItemTypeId),
                Integer.TYPE);
    }
}
