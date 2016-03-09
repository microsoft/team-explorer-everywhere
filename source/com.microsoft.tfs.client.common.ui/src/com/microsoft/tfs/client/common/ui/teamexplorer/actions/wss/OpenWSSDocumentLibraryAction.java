// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teamexplorer.actions.wss;

import org.eclipse.jface.action.IAction;

import com.microsoft.tfs.client.common.ui.teamexplorer.actions.TeamExplorerBaseAction;
import com.microsoft.tfs.client.common.ui.teamexplorer.helpers.WSSHelper;
import com.microsoft.tfs.core.clients.sharepoint.WSSDocumentLibrary;
import com.microsoft.tfs.util.Check;

public class OpenWSSDocumentLibraryAction extends TeamExplorerBaseAction {
    @Override
    public void doRun(final IAction action) {
        final WSSDocumentLibrary library = (WSSDocumentLibrary) getStructuredSelection().getFirstElement();
        Check.notNull(library, "wssDocumentLibrary"); //$NON-NLS-1$

        WSSHelper.openWSSDocumentLibrary(
            getShell(),
            getContext().getServer(),
            getContext().getCurrentProjectInfo(),
            library);
    }
}
