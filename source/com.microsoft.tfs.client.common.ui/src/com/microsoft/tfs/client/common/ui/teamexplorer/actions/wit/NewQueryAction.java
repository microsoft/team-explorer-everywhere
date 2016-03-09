// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teamexplorer.actions.wit;

import org.eclipse.jface.action.IAction;

import com.microsoft.tfs.client.common.server.TFSServer;
import com.microsoft.tfs.client.common.ui.teamexplorer.TeamExplorerEvents;
import com.microsoft.tfs.client.common.ui.teamexplorer.events.QueryItemEventArg;
import com.microsoft.tfs.client.common.ui.wit.qe.QueryEditor;
import com.microsoft.tfs.core.clients.workitem.query.QueryDocument;
import com.microsoft.tfs.core.clients.workitem.query.QueryDocumentSaveListener;
import com.microsoft.tfs.core.clients.workitem.query.QueryScope;
import com.microsoft.tfs.core.clients.workitem.queryhierarchy.QueryFolder;
import com.microsoft.tfs.core.clients.workitem.queryhierarchy.QueryItem;

public class NewQueryAction extends TeamExplorerWITBaseAction {
    @Override
    protected void doRun(final IAction action) {
        final String projectName = getContext().getCurrentProjectInfo().getName();
        final TFSServer server = getContext().getServer();
        QueryDocument queryDocument;

        if (selectedQueryItem instanceof QueryFolder) {
            final QueryFolder parent = (QueryFolder) selectedQueryItem;
            queryDocument = server.getQueryDocumentService().createNewQueryDocument(projectName, parent);
        } else if (selectedQueryItem.getParent() != null) {
            final QueryFolder parent = selectedQueryItem.getParent();
            queryDocument = server.getQueryDocumentService().createNewQueryDocument(projectName, parent);
        } else {
            queryDocument = server.getQueryDocumentService().createNewQueryDocument(projectName, QueryScope.PRIVATE);
        }

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
