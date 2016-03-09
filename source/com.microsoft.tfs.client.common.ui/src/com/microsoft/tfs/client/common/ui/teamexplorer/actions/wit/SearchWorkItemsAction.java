// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teamexplorer.actions.wit;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.IDialogConstants;

import com.microsoft.tfs.client.common.server.TFSServer;
import com.microsoft.tfs.client.common.ui.wit.dialogs.WITSearchDialog;
import com.microsoft.tfs.client.common.ui.wit.qe.QueryEditor;
import com.microsoft.tfs.client.common.ui.wit.results.QueryResultsEditor;
import com.microsoft.tfs.core.clients.workitem.WorkItemClient;
import com.microsoft.tfs.core.clients.workitem.query.QueryDocument;
import com.microsoft.tfs.core.clients.workitem.query.QueryScope;

public class SearchWorkItemsAction extends TeamExplorerWITBaseAction {
    @Override
    protected void doRun(final IAction action) {
        final WorkItemClient client = getContext().getWorkItemClient();
        final String name = getContext().getCurrentProjectInfo().getName();

        final WITSearchDialog dialog = new WITSearchDialog(getShell(), client, name, true);
        final int returnCode = dialog.open();

        if (IDialogConstants.OK_ID == returnCode || WITSearchDialog.EDIT_IN_QUERY_BUILDER == returnCode) {
            final TFSServer server = getContext().getServer();
            final String projectName =
                dialog.getSelectedProject() == null ? null : dialog.getSelectedProject().getName();

            final QueryDocument queryDocument = server.getQueryDocumentService().createNewQueryDocument(
                dialog.getWIQL(),
                projectName,
                QueryScope.PRIVATE);

            if (IDialogConstants.OK_ID == returnCode) {
                QueryResultsEditor.openEditor(server, queryDocument);
            } else {
                QueryEditor.openEditor(server, queryDocument);
            }
        }
    }
}
