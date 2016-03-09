// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.ui.egit.teamexplorer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.git.utils.GitHelpers;
import com.microsoft.tfs.client.common.ui.teamexplorer.TeamExplorerContext;
import com.microsoft.tfs.client.common.ui.teamexplorer.TeamExplorerNavigator;
import com.microsoft.tfs.client.common.ui.teamexplorer.internal.TeamExplorerHelpers;
import com.microsoft.tfs.client.common.ui.teamexplorer.internal.TeamExplorerNavigationItemConfig;
import com.microsoft.tfs.client.common.ui.teamexplorer.link.TeamExplorerBaseNavigationLink;

public class TeamExplorerEGitNavigationLink extends TeamExplorerBaseNavigationLink {

    private static final Log log = LogFactory.getLog(TeamExplorerEGitNavigationLink.class);

    @Override
    public boolean isEnabled(final TeamExplorerContext context) {
        try {
            return TeamExplorerHelpers.supportsGit(context) && GitHelpers.isEGitInstalled(false);
        } catch (final Exception e) {
            log.error("", e); //$NON-NLS-1$
            return false;
        }
    }

    @Override
    public boolean isVisible(final TeamExplorerContext context) {
        return true;
    }

    @Override
    public void onClick(
        final Shell shell,
        final TeamExplorerContext context,
        final TeamExplorerNavigator navigator,
        final TeamExplorerNavigationItemConfig parentNavigationItem) {
        try {
            TeamExplorerHelpers.showView(TeamExplorerHelpers.EGitRepoViewID);
        } catch (final Exception e) {
            log.error("", e); //$NON-NLS-1$
        }
    }
}
