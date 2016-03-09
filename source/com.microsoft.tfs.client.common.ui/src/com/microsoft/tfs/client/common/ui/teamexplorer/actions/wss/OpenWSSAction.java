// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teamexplorer.actions.wss;

import org.eclipse.jface.action.IAction;

import com.microsoft.tfs.client.common.ui.teamexplorer.actions.TeamExplorerBaseAction;
import com.microsoft.tfs.client.common.ui.teamexplorer.helpers.WSSHelper;
import com.microsoft.tfs.core.clients.sharepoint.WSSNode;
import com.microsoft.tfs.util.Check;

public class OpenWSSAction extends TeamExplorerBaseAction {
    @Override
    public void doRun(final IAction action) {
        final WSSNode wssNode = (WSSNode) getStructuredSelection().getFirstElement();
        Check.notNull(wssNode, "wssNode"); //$NON-NLS-1$

        WSSHelper.openWSSNode(getShell(), getContext().getServer(), getContext().getCurrentProjectInfo(), wssNode);
    }
}
