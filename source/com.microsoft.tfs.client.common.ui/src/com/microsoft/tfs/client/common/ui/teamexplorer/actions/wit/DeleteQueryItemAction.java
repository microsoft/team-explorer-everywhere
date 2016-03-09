// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teamexplorer.actions.wit;

import java.text.MessageFormat;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.teamexplorer.TeamExplorerEvents;
import com.microsoft.tfs.client.common.ui.teamexplorer.events.QueryFolderEventArg;
import com.microsoft.tfs.client.common.ui.teamexplorer.helpers.WorkItemHelpers;
import com.microsoft.tfs.core.clients.workitem.queryhierarchy.QueryDefinition;
import com.microsoft.tfs.core.clients.workitem.queryhierarchy.QueryFolder;
import com.microsoft.tfs.core.clients.workitem.queryhierarchy.QueryHierarchy;
import com.microsoft.tfs.core.clients.workitem.queryhierarchy.QueryItem;

public class DeleteQueryItemAction extends TeamExplorerWITBaseAction {
    @Override
    public void onSelectionChanged(final IAction action, final ISelection selection) {
        super.onSelectionChanged(action, selection);

        if (action.isEnabled() && selectionContainsRootFolder()) {
            action.setEnabled(false);
        }
    }

    @Override
    protected void doRun(final IAction action) {
        final Set<QueryFolder> parents = new HashSet<QueryFolder>();
        final QueryHierarchy queryHierarchy = getQueryHierarchy();
        String message, title;

        if (getStructuredSelection().size() == 1) {
            final QueryItem queryItem = (QueryItem) getStructuredSelection().getFirstElement();

            if (queryItem instanceof QueryFolder) {
                final String messageFormat = Messages.getString("DeleteQueryItemAction.DeleteFolderDialogTextFormat"); //$NON-NLS-1$
                message = MessageFormat.format(messageFormat, queryItem.getName());
                title = Messages.getString("DeleteQueryItemAction.DeleteFolderDialogTitle"); //$NON-NLS-1$
            } else {
                final String messageFormat = Messages.getString("DeleteQueryItemAction.DeleteQueryDialogTextFormat"); //$NON-NLS-1$
                message = MessageFormat.format(messageFormat, queryItem.getName());
                title = Messages.getString("DeleteQueryItemAction.DeleteQueryDialogTitle"); //$NON-NLS-1$
            }
        } else {
            message = Messages.getString("DeleteQueryItemAction.DeleteItemDialogText"); //$NON-NLS-1$
            title = Messages.getString("DeleteQueryItemAction.DeleteItemDialogTitle"); //$NON-NLS-1$
        }

        if (!MessageDialog.openConfirm(getShell(), title, message)) {
            return;
        }

        try {
            @SuppressWarnings("rawtypes")
            Iterator i;

            for (i = getStructuredSelection().iterator(); i.hasNext();) {
                final QueryItem queryItem = (QueryItem) i.next();

                if (queryItem instanceof QueryDefinition) {
                    WorkItemHelpers.closeEditors((QueryDefinition) queryItem);
                }

                if (!selectionContainsParents(queryItem)) {
                    parents.add(queryItem.getParent());
                    queryItem.delete();
                }
            }

            queryHierarchy.save();
        } catch (final Exception e) {
            queryHierarchy.reset();

            final String messageFormat = Messages.getString("DeleteQueryItemAction.ErrorDialogTextFormat"); //$NON-NLS-1$
            message = MessageFormat.format(messageFormat, e.getLocalizedMessage());

            MessageDialog.openError(getShell(), Messages.getString("DeleteQueryItemAction.ErrorDialogTitle"), message); //$NON-NLS-1$
            return;
        }

        for (final QueryFolder parent : parents) {
            if (parent != null) {
                final QueryFolderEventArg arg = new QueryFolderEventArg(parent);
                getContext().getEvents().notifyListener(TeamExplorerEvents.QUERY_FOLDER_CHILDREN_UPDATED, arg);
            }
        }
    }

    private boolean selectionContainsParents(final QueryItem queryItem) {
        for (QueryItem parent = queryItem.getParent(); parent != null; parent = parent.getParent()) {
            @SuppressWarnings("rawtypes")
            Iterator i;

            for (i = getStructuredSelection().iterator(); i.hasNext();) {
                final QueryItem possibleParent = (QueryItem) i.next();

                if (parent.getID().equals(possibleParent.getID())) {
                    return true;
                }
            }
        }

        return false;
    }
}
