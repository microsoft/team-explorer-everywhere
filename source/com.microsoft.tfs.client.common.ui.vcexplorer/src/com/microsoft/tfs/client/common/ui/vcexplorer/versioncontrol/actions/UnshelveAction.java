// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.vcexplorer.versioncontrol.actions;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;

import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.tasks.vc.UnshelveTask;

public class UnshelveAction extends TeamViewerAction {
    private static final Log log = LogFactory.getLog(UnshelveAction.class);

    /**
     * {@inheritDoc}
     */
    @Override
    public void doRun(final IAction action) {
        final UnshelveTask unshelveTask = new UnshelveTask(getShell(), getCurrentRepository());
        unshelveTask.run();
    }

    @Override
    protected void onSelectionChanged(final IAction action, final ISelection selection) {
        if (action.isEnabled() == false) {
            return;
        }

        action.setEnabled(
            TFSCommonUIClientPlugin.getDefault().getProductPlugin().getRepositoryManager().getDefaultRepository() != null);
    }
}
