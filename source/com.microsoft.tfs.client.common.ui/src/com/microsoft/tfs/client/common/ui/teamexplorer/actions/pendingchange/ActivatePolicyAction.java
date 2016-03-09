// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teamexplorer.actions.pendingchange;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;

import com.microsoft.tfs.client.common.ui.controls.vc.checkinpolicies.PolicyFailureData;
import com.microsoft.tfs.client.common.ui.teamexplorer.actions.TeamExplorerBaseAction;

public class ActivatePolicyAction extends TeamExplorerBaseAction {
    private PolicyFailureData selectedPolicyFailure;

    @Override
    public void onSelectionChanged(final IAction action, final ISelection selection) {
        super.onSelectionChanged(action, selection);

        if (getSelectionSize() != 1) {
            selectedPolicyFailure = null;
            action.setEnabled(false);
        } else {
            selectedPolicyFailure = (PolicyFailureData) getSelectionFirstElement();
            action.setEnabled(true);
        }
    }

    @Override
    public void doRun(final IAction action) {
        if (selectedPolicyFailure != null) {
            selectedPolicyFailure.activate();
        }
    }
}
