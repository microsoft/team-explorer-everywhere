// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teamexplorer.sections;

import org.eclipse.core.runtime.IProgressMonitor;

import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.teamexplorer.TeamExplorerContext;
import com.microsoft.tfs.client.common.ui.teamexplorer.internal.pendingchanges.PendingChangesViewModel;

public class TeamExplorerPendingChangesBaseSection extends TeamExplorerBaseSection {
    private TeamExplorerContext context;

    @Override
    public boolean initializeInBackground(final TeamExplorerContext context) {
        // Avoid launching background jobs when there won't be any interesting
        // information to display.
        return context.getDefaultRepository() != null;
    }

    @Override
    public void initialize(final IProgressMonitor monitor, final TeamExplorerContext context) {
        this.context = context;
    }

    public TeamExplorerContext getContext() {
        return context;
    }

    public PendingChangesViewModel getModel() {
        return TFSCommonUIClientPlugin.getDefault().getPendingChangesViewModel();
    }
}
