// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teamexplorer.actions.wit;

import java.text.MessageFormat;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;

import com.microsoft.tfs.client.common.server.TFSServer;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.framework.helper.MessageBoxHelpers;
import com.microsoft.tfs.client.common.ui.teamexplorer.TeamExplorerEvents;
import com.microsoft.tfs.client.common.ui.teamexplorer.events.QueryFolderEventArg;
import com.microsoft.tfs.client.common.ui.wit.dialogs.MoveQueryItemDialog;
import com.microsoft.tfs.client.common.ui.wit.dialogs.SelectQueryItemDialog;
import com.microsoft.tfs.core.clients.workitem.project.Project;
import com.microsoft.tfs.core.clients.workitem.queryhierarchy.QueryFolder;
import com.microsoft.tfs.core.clients.workitem.queryhierarchy.QueryHierarchy;
import com.microsoft.tfs.core.clients.workitem.queryhierarchy.QueryItem;
import com.microsoft.tfs.core.clients.workitem.queryhierarchy.QueryItemType;
import com.microsoft.tfs.util.GUID;

public class MoveQueryItemAction extends TeamExplorerWITBaseAction {
    @Override
    public void onSelectionChanged(final IAction action, final ISelection selection) {
        super.onSelectionChanged(action, selection);

        if (action.isEnabled() && selectionContainsRootFolder()) {
            action.setEnabled(false);
        }
    }

    @Override
    protected void doRun(final IAction action) {
        final TFSServer server = getContext().getServer();
        final QueryHierarchy queryHierarchy = getQueryHierarchy();

        final QueryFolder newParent;
        final QueryItem[] queryItems;

        if (getStructuredSelection().size() == 1) {
            final QueryItem queryItem = selectedQueryItem;

            final Project[] projects = new Project[] {
                queryItem.getProject()
            };

            final MoveQueryItemDialog moveDialog = new MoveQueryItemDialog(getShell(), server, projects, queryItem);

            if (moveDialog.open() != IDialogConstants.OK_ID) {
                return;
            }

            newParent = moveDialog.getParent();

            queryItems = new QueryItem[] {
                queryItem
            };

            try {
                queryItem.setName(moveDialog.getName());
            } catch (final Exception e) {
                queryHierarchy.reset();

                final String messageFormat = Messages.getString("MoveQueryItemAction.CouldNotMoveDialogTextFormat"); //$NON-NLS-1$
                final String message = MessageFormat.format(messageFormat, e.getLocalizedMessage());

                MessageDialog.openError(
                    getShell(),
                    Messages.getString("MoveQueryItemAction.CouldNotMoveDialogTitle"), //$NON-NLS-1$
                    message);
                return;
            }
        } else {
            queryItems = new QueryItem[getStructuredSelection().size()];

            int idx = 0;
            Project project = null;
            boolean multipleProjects = false;

            for (final Iterator i = getStructuredSelection().iterator(); i.hasNext(); idx++) {
                queryItems[idx] = (QueryItem) i.next();
                if (project == null) {
                    project = queryItems[idx].getProject();
                } else {
                    if (project.getID() != queryItems[idx].getProject().getID()) {
                        multipleProjects = true;
                        break;
                    }
                }
            }

            if (multipleProjects) {
                MessageBoxHelpers.errorMessageBox(
                    getShell(),
                    Messages.getString("MoveQueryItemAction.CannotMoveDialogTitle"), //$NON-NLS-1$
                    Messages.getString("MoveQueryItemAction.CannotMoveDialogText")); //$NON-NLS-1$
                return;
            }

            final Project[] projects = new Project[] {
                project
            };

            final SelectQueryItemDialog moveDialog = new SelectQueryItemDialog(
                getShell(),
                server,
                projects,
                getBestParent(queryItems),
                QueryItemType.QUERY_FOLDER);

            if (moveDialog.open() != IDialogConstants.OK_ID) {
                return;
            }

            newParent = (QueryFolder) moveDialog.getSelectedQueryItem();
        }

        final Set<QueryFolder> oldParents = new HashSet<QueryFolder>();

        try {
            for (final QueryItem queryItem : queryItems) {
                if (!oldParents.contains(queryItem.getParent())) {
                    oldParents.add(queryItem.getParent());
                }
                newParent.add(queryItem);
            }

            queryHierarchy.save();
        } catch (final Exception e) {
            queryHierarchy.reset();

            final String messageFormat = Messages.getString("MoveQueryItemAction.CouldNotMoveDialogTextFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, e.getLocalizedMessage());

            MessageDialog.openError(
                getShell(),
                Messages.getString("MoveQueryItemAction.CouldNotMoveDialogTitle"), //$NON-NLS-1$
                message);
            return;
        }

        /* Refresh the parents of the old location */
        for (final QueryFolder oldParent : oldParents) {
            final QueryFolderEventArg arg = new QueryFolderEventArg(oldParent);
            getContext().getEvents().notifyListener(TeamExplorerEvents.QUERY_FOLDER_CHILDREN_UPDATED, arg);
        }

        /* Refresh the new location */
        if (newParent != null) {
            final QueryFolderEventArg arg = new QueryFolderEventArg(newParent);
            getContext().getEvents().notifyListener(TeamExplorerEvents.QUERY_FOLDER_CHILDREN_UPDATED, arg);
        }
    }

    private static final QueryFolder getBestParent(final QueryItem[] items) {
        QueryFolder parent = null;

        for (int i = 0; i < items.length; i++) {
            if (parent == null) {
                parent = items[i].getParent();
            } else if (isParent(parent, items[i].getParent())) {
                parent = items[i].getParent();
            }
        }

        return parent;
    }

    private static boolean isParent(final QueryItem item, final QueryFolder possibleParent) {
        if (item.getID().equals(possibleParent.getID())) {
            return true;
        }

        if (!item.getID().equals(GUID.EMPTY)) {
            return isParent(item.getParent(), possibleParent);
        }

        return false;
    }
}
