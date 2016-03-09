// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teamexplorer.actions.pendingchange;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;

import com.microsoft.tfs.client.common.ui.framework.compare.CompareUIType;
import com.microsoft.tfs.client.common.ui.teamexplorer.helpers.PendingChangesHelpers;
import com.microsoft.tfs.util.Check;

public class CompareWithLatestVersionAction extends CompareWithBaseAction {
    @Override
    public void onSelectionChanged(final IAction action, final ISelection selection) {
        super.onSelectionChanged(action, selection);

        if (action.isEnabled() && !PendingChangesHelpers.canCompareWithLatestVersion(selectedPendingChange)) {
            action.setEnabled(false);
        }

        if (PendingChangesHelpers.containsSymlinkChange(selectedPendingChange)) {
            action.setEnabled(false);
        }
    }

    @Override
    public void doRun(final IAction action) {
        Check.notNull(selectedPendingChange, "selectedPendingChange"); //$NON-NLS-1$

        PendingChangesHelpers.compareWithLatestVersion(
            getShell(),
            getContext().getDefaultRepository(),
            selectedPendingChange,
            CompareUIType.EDITOR);
    }
}
