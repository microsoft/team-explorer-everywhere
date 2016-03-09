// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.metadata.impl;

import com.microsoft.tfs.core.clients.workitem.internal.metadata.dao.HierarchyPropertiesTable;

public class HierarchyPropertiesTableImpl extends BaseMetadataDAO implements HierarchyPropertiesTable {
    @Override
    public String getValue(final int propId) {
        return getConnection().createStatement(
            "select Value from HierarchyProperties where PropID = ?").executeStringQuery( //$NON-NLS-1$
                new Object[] {
                    new Integer(propId)
        });
    }
}
