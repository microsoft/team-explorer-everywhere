// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teamexplorer.link;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.ui.teamexplorer.TeamExplorerContext;
import com.microsoft.tfs.client.common.ui.teamexplorer.TeamExplorerNavigator;
import com.microsoft.tfs.client.common.ui.teamexplorer.helpers.PendingChangesHelpers;
import com.microsoft.tfs.client.common.ui.teamexplorer.internal.TeamExplorerNavigationItemConfig;

public class TeamExplorerDetectChangesNavigationLink extends TeamExplorerBaseNavigationLink {

    private static final Log log = LogFactory.getLog(TeamExplorerDetectChangesNavigationLink.class);

    @Override
    public boolean isEnabled(final TeamExplorerContext context) {
        try {
            return context.isConnectedToCollection()
                && context.getDefaultRepository().getWorkspace().isLocalWorkspace();
        } catch (final Exception e) {
            log.error(e);
            return false;
        }
    }

    @Override
    public boolean isVisible(final TeamExplorerContext context) {
        try {
            return context.isConnectedToCollection()
                && context.getDefaultRepository().getWorkspace().isLocalWorkspace();
        } catch (final Exception e) {
            log.error(e);
            return false;
        }
    }

    @Override
    public void onClick(
        final Shell shell,
        final TeamExplorerContext context,
        final TeamExplorerNavigator navigator,
        final TeamExplorerNavigationItemConfig parentNavigationItem) {
        try {
            PendingChangesHelpers.detectLocalChanges(shell, context.getDefaultRepository());
            navigator.navigateToItem(parentNavigationItem);
        } catch (final Exception e) {
            log.error(e);
        }
    }
}
