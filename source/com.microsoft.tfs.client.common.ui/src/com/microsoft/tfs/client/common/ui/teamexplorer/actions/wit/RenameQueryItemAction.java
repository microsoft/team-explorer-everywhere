// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teamexplorer.actions.wit;

import java.text.MessageFormat;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.dialogs.wit.SetQueryItemNameDialog;
import com.microsoft.tfs.client.common.ui.teamexplorer.TeamExplorerEvents;
import com.microsoft.tfs.client.common.ui.teamexplorer.events.QueryFolderEventArg;
import com.microsoft.tfs.client.common.ui.teamexplorer.events.QueryItemEventArg;
import com.microsoft.tfs.client.common.ui.wit.query.BaseQueryDocumentEditor;
import com.microsoft.tfs.core.clients.workitem.query.QueryDocument;
import com.microsoft.tfs.core.clients.workitem.queryhierarchy.QueryDefinition;
import com.microsoft.tfs.core.clients.workitem.queryhierarchy.QueryHierarchy;
import com.microsoft.tfs.core.clients.workitem.queryhierarchy.QueryItem;

public class RenameQueryItemAction extends TeamExplorerWITBaseAction {
    @Override
    public void onSelectionChanged(final IAction action, final ISelection selection) {
        super.onSelectionChanged(action, selection);

        if (action.isEnabled() && selectionContainsRootFolder()) {
            action.setEnabled(false);
        }
    }

    @Override
    protected void doRun(final IAction action) {
        final QueryHierarchy queryHierarchy = getQueryHierarchy();
        final QueryItem queryItem = selectedQueryItem;

        final SetQueryItemNameDialog nameDialog = new SetQueryItemNameDialog(getShell());
        nameDialog.setOriginalName(queryItem.getName());
        nameDialog.setParent(queryItem.getParent());

        if (nameDialog.open() != IDialogConstants.OK_ID) {
            return;
        }

        try {
            queryItem.setName(nameDialog.getName());
            queryHierarchy.save();

            /*
             * Update any open editors if this is a query.
             */
            if (queryItem instanceof QueryDefinition) {
                final QueryDefinition queryDefinition = (QueryDefinition) queryItem;
                final IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();

                for (final IEditorReference editorReference : page.getEditorReferences()) {
                    final IWorkbenchPart part = editorReference.getPart(false);
                    if (part instanceof BaseQueryDocumentEditor) {
                        final BaseQueryDocumentEditor baseQueryDocumentEditor = (BaseQueryDocumentEditor) part;
                        final QueryDocument queryDocument = baseQueryDocumentEditor.getQueryDocument();

                        if (queryDefinition.getID().equals(queryDocument.getGUID())) {
                            baseQueryDocumentEditor.onQueryDocumentRenamed(queryDefinition.getName());
                        }
                    }
                }
            }
        } catch (final Exception e) {
            queryHierarchy.reset();

            final String messageFormat = Messages.getString("RenameQueryItemAction.ErrorDialogTextFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, e.getLocalizedMessage());
            MessageDialog.openError(getShell(), Messages.getString("RenameQueryItemAction.ErrorDialogTitle"), message); //$NON-NLS-1$
            return;
        }

        final QueryItemEventArg queryItemArg = new QueryItemEventArg(queryItem);
        getContext().getEvents().notifyListener(TeamExplorerEvents.QUERY_ITEM_RENAMED, queryItemArg);

        final QueryFolderEventArg queryFolderArg = new QueryFolderEventArg(queryItem.getParent());
        getContext().getEvents().notifyListener(TeamExplorerEvents.QUERY_FOLDER_CHILDREN_UPDATED, queryFolderArg);
    }
}
