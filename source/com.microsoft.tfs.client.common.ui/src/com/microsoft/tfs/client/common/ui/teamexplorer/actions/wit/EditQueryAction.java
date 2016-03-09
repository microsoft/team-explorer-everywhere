// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teamexplorer.actions.wit;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;

import com.microsoft.tfs.client.common.server.TFSServer;
import com.microsoft.tfs.client.common.ui.teamexplorer.TeamExplorerEvents;
import com.microsoft.tfs.client.common.ui.teamexplorer.events.QueryItemEventArg;
import com.microsoft.tfs.client.common.ui.teamexplorer.helpers.WorkItemHelpers;
import com.microsoft.tfs.client.common.ui.wit.qe.QueryEditor;
import com.microsoft.tfs.core.clients.workitem.query.QueryDocument;
import com.microsoft.tfs.core.clients.workitem.query.QueryDocumentSaveListener;
import com.microsoft.tfs.core.clients.workitem.query.StoredQuery;
import com.microsoft.tfs.core.clients.workitem.queryhierarchy.QueryDefinition;
import com.microsoft.tfs.core.clients.workitem.queryhierarchy.QueryItem;

public class EditQueryAction extends TeamExplorerWITBaseAction {
    private QueryDefinition queryDefinition;
    private StoredQuery storedQuery;

    @Override
    public void onSelectionChanged(final IAction action, final ISelection selection) {
        super.onSelectionChanged(action, selection);

        if (action.isEnabled()) {
            queryDefinition = (QueryDefinition) selectedQueryItem;
            storedQuery = WorkItemHelpers.createStoredQueryFromDefinition(queryDefinition);

            action.setEnabled(storedQuery.isParsable());
        }
    }

    @Override
    protected void doRun(final IAction action) {
        final TFSServer server = getContext().getServer();

        final QueryDocument queryDocument = server.getQueryDocumentService().getQueryDocumentForStoredQuery(
            queryDefinition.getProject(),
            storedQuery.getQueryGUID());

        queryDocument.addSaveListener(new QueryDocumentSaveListener() {
            @Override
            public void onQueryDocumentSaved(final QueryDocument queryDocument) {
                final QueryItem savedToParent = getQueryHierarchy().find(queryDocument.getParentGUID());
                if (savedToParent != null) {
                    final QueryItemEventArg arg = new QueryItemEventArg(savedToParent);
                    getContext().getEvents().notifyListener(TeamExplorerEvents.QUERY_ITEM_UPDATED, arg);
                }
            }
        });

        QueryEditor.openEditor(server, queryDocument);
    }
}
