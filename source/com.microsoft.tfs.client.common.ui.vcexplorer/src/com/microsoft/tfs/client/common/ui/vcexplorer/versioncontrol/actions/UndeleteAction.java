// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.vcexplorer.versioncontrol.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;

import com.microsoft.tfs.client.common.commands.vc.UndeleteCommand;
import com.microsoft.tfs.client.common.framework.resources.command.ResourceChangingCommand;
import com.microsoft.tfs.client.common.ui.framework.command.UICommandExecutorFactory;
import com.microsoft.tfs.client.common.ui.vc.tfsitem.TFSItem;
import com.microsoft.tfs.client.common.ui.vc.tfsitem.TFSItemUtils;
import com.microsoft.tfs.client.common.ui.vcexplorer.versioncontrol.actions.helper.ActionEnablementHelper;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;

/**
 * Action called to check out a file or folder.
 */
public class UndeleteAction extends TeamViewerAction {
    private TFSItem[] items;

    /**
     * {@inheritDoc}
     */
    @Override
    public void doRun(final IAction action) {
        final ItemSpec[] itemSpecs = TFSItemUtils.getItemSpecs(items);

        final UndeleteCommand command = new UndeleteCommand(getCurrentRepository(), itemSpecs);

        UICommandExecutorFactory.newUICommandExecutor(getShell()).execute(new ResourceChangingCommand(command));
    }

    @Override
    protected void onSelectionChanged(final IAction action, final ISelection selection) {
        if (action.isEnabled() == false) {
            return;
        }

        items = (TFSItem[]) adaptSelectionToArray(TFSItem.class);

        action.setEnabled(ActionEnablementHelper.selectionContainsNonDeletedItem(selection) == false);
    }
}
