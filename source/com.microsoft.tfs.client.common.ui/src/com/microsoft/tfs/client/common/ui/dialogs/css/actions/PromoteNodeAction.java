// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.dialogs.css.actions;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.controls.CommonStructureControl;
import com.microsoft.tfs.core.clients.commonstructure.CSSNode;

public class PromoteNodeAction extends CSSNodeAction {

    public PromoteNodeAction(final CommonStructureControl cssControl) {
        super(
            cssControl,
            Messages.getString("PromoteNodeAction.ActionText"), //$NON-NLS-1$
            Messages.getString("PromoteNodeAction.ActionDescription"), //$NON-NLS-1$
            "icons/promote_node.gif"); //$NON-NLS-1$
    }

    @Override
    public void doRun() {
        final CSSNode selectedNode = getSelectedNode();
        if (selectedNode != null
            && selectedNode.getParentNode() != null
            && selectedNode.getParentNode().getParentNode() != null) {
            getCSSControl().moveNode(selectedNode, selectedNode.getParentNode().getParentNode());
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
        if (cssNode.getParentNode().getParentURI() == null || cssNode.getParentNode().getParentURI().length() == 0) {
            return false;
        }
        return true;
    }

}
