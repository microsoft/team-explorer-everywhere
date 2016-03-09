// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.dialogs.css.actions;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.controls.CommonStructureControl;
import com.microsoft.tfs.core.clients.commonstructure.CSSNode;

public class MoveDownNodeAction extends CSSNodeAction {

    public MoveDownNodeAction(final CommonStructureControl cssControl) {
        super(
            cssControl,
            Messages.getString("MoveDownNodeAction.ActionText"), //$NON-NLS-1$
            Messages.getString("MoveDownNodeAction.ActionDescription"), //$NON-NLS-1$
            "icons/down.gif"); //$NON-NLS-1$
    }

    @Override
    public void doRun() {
        getCSSControl().reorderNode(getSelectedNode(), 1);
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
        return cssNode.getParentNode().indexOfChild(cssNode) < (cssNode.getParentNode().getChildrenSize() - 1);
    }

}
