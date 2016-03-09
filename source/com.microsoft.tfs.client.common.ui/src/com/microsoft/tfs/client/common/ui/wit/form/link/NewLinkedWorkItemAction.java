// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.wit.form.link;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.server.TFSServer;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.helpers.WorkItemEditorHelper;
import com.microsoft.tfs.core.clients.workitem.WorkItem;
import com.microsoft.tfs.core.clients.workitem.WorkItemStateAdapter;
import com.microsoft.tfs.core.clients.workitem.WorkItemStateListener;
import com.microsoft.tfs.core.clients.workitem.WorkItemUtils;
import com.microsoft.tfs.core.clients.workitem.exceptions.WorkItemLinkValidationException;
import com.microsoft.tfs.core.clients.workitem.form.WIFormLinksControlOptions;

/***
 * Action to prompt for and create a new linked work item.
 */
public class NewLinkedWorkItemAction extends Action {
    private final Shell shell;
    private final TFSServer server;
    private final WorkItem workItem;
    private final WIFormLinksControlOptions options;

    public NewLinkedWorkItemAction(
        final Shell shell,
        final TFSServer server,
        final WorkItem workItem,
        final WIFormLinksControlOptions options) {
        this.shell = shell;
        this.server = server;
        this.workItem = workItem;
        this.options = options;
    }

    @Override
    public void run() {
        try {
            final NewLinkedWorkItemDialog dialog = new NewLinkedWorkItemDialog(shell, workItem, options);

            if (dialog.open() == IDialogConstants.OK_ID) {
                final WorkItem relatedWorkItem = WorkItemUtils.createRelatedWorkItem(
                    workItem,
                    dialog.getSelectedWorkItemType(),
                    dialog.getSelectedLinkTypeID(),
                    dialog.getTitle(),
                    dialog.getComment(),
                    false);

                // Listen for the new work item to be saved. Refresh this work
                // item when the new work item is saved if this work item is not
                // dirty.
                final WorkItemStateListener workItemStateListener = new WorkItemStateAdapter() {
                    @Override
                    public void saved(final WorkItem newWorkItem) {
                        if (!workItem.isDirty()) {
                            workItem.syncToLatest();
                        }
                    }
                };

                relatedWorkItem.addWorkItemStateListener(workItemStateListener);
                WorkItemEditorHelper.openEditor(server, relatedWorkItem);
            }
        } catch (final WorkItemLinkValidationException e) {
            MessageDialog.openError(
                shell,
                Messages.getString("WorkItemLinksControl.ErrorDialogTitle"), //$NON-NLS-1$
                e.getLocalizedMessage());
        }
    }
}
