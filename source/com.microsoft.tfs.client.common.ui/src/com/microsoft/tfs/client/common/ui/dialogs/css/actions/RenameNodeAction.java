// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.dialogs.css.actions;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.controls.CommonStructureControl;
import com.microsoft.tfs.client.common.ui.dialogs.generic.StringInputDialog;
import com.microsoft.tfs.core.clients.commonstructure.CSSNode;

public class RenameNodeAction extends CSSNodeAction {

    public RenameNodeAction(final CommonStructureControl cssControl) {
        super(cssControl, Messages.getString("RenameNodeAction.ActionText"), null, null); //$NON-NLS-1$
    }

    @Override
    public void doRun() {
        final CSSNode selectedNode = getSelectedNode();

        /* No cell editor support available */
        if (SWT.getVersion() < 3100) {
            final StringInputDialog nameDialog = new StringInputDialog(
                getCSSControl().getShell(),
                Messages.getString("RenameNodeAction.StringInputLabelText"), //$NON-NLS-1$
                selectedNode.getName(),
                Messages.getString("RenameNodeAction.StringInputDialogTitle"), //$NON-NLS-1$
                "rename-css-node"); //$NON-NLS-1$

            if (nameDialog.open() != IDialogConstants.OK_ID) {
                return;
            }

            final String newName = nameDialog.getInput().trim();

            getCSSControl().renameNode(selectedNode, newName);

            return;
        }

        getCSSControl().getTreeViewer().editElement(selectedNode, 0);
    }

    @Override
    protected boolean computeEnablement(final CSSNode cssNode) {
        if (cssNode == null) {
            return false;
        }
        if (cssNode.getURI() == null || cssNode.getURI().length() == 0) {
            return false;
        }
        return cssNode.getParentURI() != null && cssNode.getParentURI().length() > 0;
    }

}
