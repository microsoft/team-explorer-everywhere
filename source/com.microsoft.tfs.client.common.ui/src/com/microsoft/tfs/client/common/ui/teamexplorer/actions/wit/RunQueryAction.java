// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teamexplorer.actions.wit;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;

import com.microsoft.tfs.client.common.ui.teamexplorer.helpers.WorkItemHelpers;
import com.microsoft.tfs.core.clients.workitem.query.StoredQuery;
import com.microsoft.tfs.core.clients.workitem.queryhierarchy.QueryDefinition;

public class RunQueryAction extends TeamExplorerWITBaseAction {
    private QueryDefinition queryDefinition;
    private StoredQuery storedQuery;

    @Override
    public void onSelectionChanged(final IAction action, final ISelection selection) {
        super.onSelectionChanged(action, selection);

        if (!action.isEnabled()) {
            return;
        }

        /*
         * Node can be null when the view is starting, we may get a selection
         * changed event (with null node) when initial draw.
         */
        if (selectedQueryItem == null || !(selectedQueryItem instanceof QueryDefinition)) {
            action.setEnabled(false);
            return;
        }

        queryDefinition = (QueryDefinition) selectedQueryItem;
        storedQuery = WorkItemHelpers.createStoredQueryFromDefinition(queryDefinition);

        action.setEnabled(storedQuery.isParsable());
    }

    @Override
    protected void doRun(final IAction action) {
        WorkItemHelpers.runQuery(getShell(), getContext().getServer(), queryDefinition.getProject(), storedQuery);
    }
}
