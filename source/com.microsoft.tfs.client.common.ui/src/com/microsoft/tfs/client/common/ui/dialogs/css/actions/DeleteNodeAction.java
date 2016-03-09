// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.dialogs.css.actions;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.controls.CommonStructureControl;
import com.microsoft.tfs.core.clients.commonstructure.CSSNode;

public class DeleteNodeAction extends CSSNodeAction {

    public DeleteNodeAction(final CommonStructureControl cssControl) {
        super(
            cssControl,
            Messages.getString("DeleteNodeAction.ActionText"), //$NON-NLS-1$
            Messages.getString("DeleteNodeAction.ActionDescription"), //$NON-NLS-1$
            "icons/delete_red.gif"); //$NON-NLS-1$
    }

    @Override
    public void doRun() {
        getCSSControl().deleteNode(getSelectedNode());
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
