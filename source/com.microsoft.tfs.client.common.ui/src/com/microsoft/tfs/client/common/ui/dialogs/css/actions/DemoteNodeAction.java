// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.dialogs.css.actions;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.controls.CommonStructureControl;
import com.microsoft.tfs.core.clients.commonstructure.CSSNode;

public class DemoteNodeAction extends CSSNodeAction {

    public DemoteNodeAction(final CommonStructureControl cssControl) {
        super(
            cssControl,
            Messages.getString("DemoteNodeAction.ActionText"), //$NON-NLS-1$
            Messages.getString("DemoteNodeAction.ActionDescription"), //$NON-NLS-1$
            "icons/demote_node.gif"); //$NON-NLS-1$
    }

    @Override
    public void doRun() {
        final CSSNode selectedNode = getSelectedNode();
        if (selectedNode != null && selectedNode.getParentNode() != null) {
            final CSSNode parent = selectedNode.getParentNode();
            final CSSNode sibling = parent.getChildAt(parent.indexOfChild(selectedNode) - 1);
            getCSSControl().moveNode(selectedNode, sibling);
        }
    }

    @Override
    protected boolean computeEnablement(final CSSNode cssNode) {
        if (cssNode == null) {
            return false;
        }
        if (cssNode.getURI() == null || cssNode.getURI().length() == 0) {
            return false;
        }
        if (cssNode.getParentURI() == null || cssNode.getParentURI().length() == 0) {
            return false;
        }
        if (cssNode.getParentNode().indexOfChild(cssNode) > 0) {
            return true;
        }
        return false;
    }

}
