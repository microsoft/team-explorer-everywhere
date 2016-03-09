// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.dialogs.vc.candidates;

import org.eclipse.jface.viewers.IStructuredSelection;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.dialogs.vc.PromoteCandidateChangesDialog;

public class SelectAllAction extends CandidateAction {
    public SelectAllAction(final PromoteCandidateChangesDialog dialog) {
        super(dialog);
        setText(Messages.getString("SelectAllAction.SelectAllActionText")); //$NON-NLS-1$
    }

    @Override
    public void doRun() {
        dialog.getTable().selectAll();
    }

    @Override
    protected boolean computeEnablement(final IStructuredSelection selection) {
        return true;
    }
}
