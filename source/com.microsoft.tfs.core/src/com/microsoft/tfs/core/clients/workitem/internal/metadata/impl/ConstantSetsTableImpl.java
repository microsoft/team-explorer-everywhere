// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.metadata.impl;

import com.microsoft.tfs.core.clients.workitem.internal.metadata.dao.ConstantSetsTable;

public class ConstantSetsTableImpl extends BaseMetadataDAO implements ConstantSetsTable {
    @Override
    public int[] getConstantIDsForParentID(final int parentId) {
        return (int[]) getConnection().createStatement(
            "select ConstID from ConstantSets where ParentID = ?").executeQueryForPrimitiveArray( //$NON-NLS-1$
                new Integer(parentId),
                Integer.TYPE);
    }
}
