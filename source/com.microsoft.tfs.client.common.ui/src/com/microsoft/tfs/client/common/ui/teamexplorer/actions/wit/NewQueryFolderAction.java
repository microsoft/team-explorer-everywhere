// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teamexplorer.actions.wit;

import java.text.MessageFormat;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;

import com.microsoft.tfs.client.common.codemarker.CodeMarker;
import com.microsoft.tfs.client.common.codemarker.CodeMarkerDispatch;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.dialogs.wit.SetQueryItemNameDialog;
import com.microsoft.tfs.client.common.ui.teamexplorer.TeamExplorerEvents;
import com.microsoft.tfs.client.common.ui.teamexplorer.events.QueryFolderEventArg;
import com.microsoft.tfs.core.clients.workitem.queryhierarchy.QueryFolder;
import com.microsoft.tfs.util.Check;

public class NewQueryFolderAction extends TeamExplorerWITBaseAction {
    public static final CodeMarker CODEMARKER_CREATE_COMPLETE = new CodeMarker(
        "com.microsoft.tfs.client.common.ui.wit.teamexplorer.actions.NewQueryFolderAction#createComplete"); //$NON-NLS-1$

    @Override
    public void onSelectionChanged(final IAction action, final ISelection selection) {
        action.setEnabled(true);

        super.onSelectionChanged(action, selection);

        if (getSelectionSize() > 1 || !(selectedQueryItem instanceof QueryFolder)) {
            action.setEnabled(false);
        }
    }

    @Override
    protected void doRun(final IAction action) {
        Check.isTrue(selectedQueryItem instanceof QueryFolder, "selectedQueryItem instanceof QueryFolder"); //$NON-NLS-1$
        final QueryFolder queryFolder = (QueryFolder) selectedQueryItem;

        final SetQueryItemNameDialog nameDialog = new SetQueryItemNameDialog(getShell());
        nameDialog.setParent(queryFolder);

        if (nameDialog.open() != IDialogConstants.OK_ID) {
            return;
        }

        try {
            queryFolder.newFolder(nameDialog.getName());
            CodeMarkerDispatch.dispatch(CODEMARKER_CREATE_COMPLETE);
        } catch (final Exception e) {
            final String messageFormat = Messages.getString("NewQueryFolderAction.ErrorDialogTextFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, e.getLocalizedMessage());
            MessageDialog.openError(getShell(), Messages.getString("NewQueryFolderAction.ErrorDialogTitle"), message); //$NON-NLS-1$
            return;
        }

        try {
            getQueryHierarchy().save();
        } catch (final Exception e) {
            getQueryHierarchy().reset();

            final String messageFormat = Messages.getString("NewQueryFolderAction.ErrorDialogTextFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, e.getLocalizedMessage());
            MessageDialog.openError(getShell(), Messages.getString("NewQueryFolderAction.ErrorDialogTitle"), message); //$NON-NLS-1$
            return;
        }

        final QueryFolderEventArg arg = new QueryFolderEventArg(queryFolder);
        getContext().getEvents().notifyListener(TeamExplorerEvents.QUERY_FOLDER_CHILDREN_UPDATED, arg);
    }
}
