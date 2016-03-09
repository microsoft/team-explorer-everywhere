// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.dialogs.css.actions;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.controls.CommonStructureControl;
import com.microsoft.tfs.client.common.ui.dialogs.generic.StringInputDialog;
import com.microsoft.tfs.core.clients.commonstructure.CSSNode;

public class NewNodeAction extends CSSNodeAction {

    public NewNodeAction(final CommonStructureControl selectionProvider) {
        super(
            selectionProvider,
            Messages.getString("NewNodeAction.ActionText"), //$NON-NLS-1$
            Messages.getString("NewNodeAction.ActionDescription"), //$NON-NLS-1$
            "icons/new_node.gif"); //$NON-NLS-1$
    }

    @Override
    public void doRun() {
        boolean useCellEditor = true;

        final CSSNode selectedNode = getSelectedNode();
        String newName = getUniqueNodeName(selectedNode);

        /* No cell editor support fallback */
        if (SWT.getVersion() < 3100) {
            final StringInputDialog nameDialog = new StringInputDialog(
                getCSSControl().getShell(),
                Messages.getString("NewNodeAction.StringInputLabelText"), //$NON-NLS-1$
                newName,
                Messages.getString("NewNodeAction.StringInputDialogTitle"), //$NON-NLS-1$
                "new-css-node"); //$NON-NLS-1$

            if (nameDialog.open() != IDialogConstants.OK_ID) {
                return;
            }

            newName = nameDialog.getInput().trim();

            useCellEditor = false;
        }

        final CSSNode newNode = new CSSNode(selectedNode.getStructureType(), ""); //$NON-NLS-1$
        newNode.setName(newName);
        selectedNode.addChild(newNode);
        getCSSControl().setNewNode(newNode);

        if (!useCellEditor) {
            getCSSControl().newNode(newNode, newName);
        }

        final TreeViewer viewer = getCSSControl().getTreeViewer();
        viewer.refresh(selectedNode);

        viewer.setSelection(new StructuredSelection(newNode));

        if (useCellEditor) {
            viewer.editElement(newNode, 0);
            viewer.editElement(newNode, 0);
        }
    }

    private String getUniqueNodeName(final CSSNode selectedNode) {
        final String baseName = getCSSControl().getRootNode().getName() + " "; //$NON-NLS-1$

        String uniqueNodeName = baseName + "0"; //$NON-NLS-1$
        boolean nameFound = false;
        final CSSNode[] children = (CSSNode[]) selectedNode.getChildren();
        int nodeNumber = 0;
        while (!nameFound) {
            uniqueNodeName = baseName + nodeNumber++;
            nameFound = true;
            for (int i = 0; i < children.length; i++) {
                if (uniqueNodeName.equalsIgnoreCase(children[i].getName())) {
                    nameFound = false;
                }
            }
        }

        return uniqueNodeName;
    }

    @Override
    protected boolean computeEnablement(final CSSNode cssNode) {
        if (cssNode == null) {
            return false;
        }
        if (cssNode.getURI() == null || cssNode.getURI().length() == 0) {
            return false;
        }
        return getSelectionSize() > 0;
    }

}
