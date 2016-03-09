// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.vcexplorer.teamexplorer;

import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import com.microsoft.tfs.client.common.item.ServerItemPath;
import com.microsoft.tfs.client.common.ui.teamexplorer.TeamExplorerContext;
import com.microsoft.tfs.client.common.ui.teamexplorer.helpers.WebAccessHelper;
import com.microsoft.tfs.client.common.ui.teamexplorer.items.TeamExplorerBaseNavigationItem;
import com.microsoft.tfs.client.common.ui.vcexplorer.versioncontrol.VersionControlEditor;
import com.microsoft.tfs.client.common.ui.vcexplorer.versioncontrol.VersionControlEditorInput;
import com.microsoft.tfs.core.clients.commonstructure.ProjectInfo;
import com.microsoft.tfs.core.clients.versioncontrol.SourceControlCapabilityFlags;
import com.microsoft.tfs.util.StringUtil;

/**
 * The team explorer navigation item for source control explorer. Note this item
 * is not navigatable, just need to directly open source control explorer.
 */
public class TeamExplorerVersionControlNavigationItem extends TeamExplorerBaseNavigationItem {

    public static final String ID =
        "com.microsoft.tfs.client.common.ui.vcexplorer.teamexplorer.TeamExplorerVersionControlNavigationItem"; //$NON-NLS-1$

    @Override
    public boolean isVisible(final TeamExplorerContext context) {
        if (context.isConnected()) {
            final SourceControlCapabilityFlags flags = context.getSourceControlCapability();
            return flags.contains(SourceControlCapabilityFlags.TFS);
        } else {
            return context.isConnectedToCollection();
        }
    }

    @Override
    public void onClick(final TeamExplorerContext context) {
        final IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();

        try {
            page.openEditor(new VersionControlEditorInput(), VersionControlEditor.ID);

            final ProjectInfo projectInfo = context.getCurrentProjectInfo();
            final ServerItemPath versionControlPath =
                ServerItemPath.ROOT.combine(projectInfo != null ? projectInfo.getName() : StringUtil.EMPTY);

            if (VersionControlEditor.getCurrent() != null) {
                VersionControlEditor.getCurrent().setSelectedFolder(versionControlPath);
            }
        } catch (final PartInitException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean canOpenInWeb() {
        return true;
    }

    @Override
    public void openInWeb(final TeamExplorerContext context) {
        WebAccessHelper.openVersionControl(context);
    }
}
