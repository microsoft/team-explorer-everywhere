// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.metadata.impl;

import com.microsoft.tfs.core.clients.workitem.internal.metadata.dao.ActionsTable;

public class ActionsTableImpl extends BaseMetadataDAO implements ActionsTable {
    @Override
    public String getNextStateForAction(final String currentState, final String action, final int workItemTypeID) {
        final Integer currentStateId = getMetadata().getConstantsTable().getIDByConstant(currentState);

        final String sql = "select ToStateConstID from Actions where " //$NON-NLS-1$
            + "lower(Name) = lower(?) and " //$NON-NLS-1$
            + "WorkItemTypeID = ? and " //$NON-NLS-1$
            + "FromStateConstID = ?"; //$NON-NLS-1$

        final Integer toStateId = getConnection().createStatement(sql).executeIntQuery(new Object[] {
            action,
            new Integer(workItemTypeID),
            currentStateId
        });

        if (toStateId == null) {
            return null;
        }

        return getMetadata().getConstantsTable().getConstantByID(toStateId.intValue());
    }
}
