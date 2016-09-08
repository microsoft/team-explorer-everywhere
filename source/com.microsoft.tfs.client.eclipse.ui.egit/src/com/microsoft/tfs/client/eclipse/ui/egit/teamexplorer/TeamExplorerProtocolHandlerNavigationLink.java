// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.ui.egit.teamexplorer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.git.utils.GitHelpers;
import com.microsoft.tfs.client.common.ui.teamexplorer.TeamExplorerContext;
import com.microsoft.tfs.client.common.ui.teamexplorer.TeamExplorerNavigator;
import com.microsoft.tfs.client.common.ui.teamexplorer.internal.TeamExplorerHelpers;
import com.microsoft.tfs.client.common.ui.teamexplorer.internal.TeamExplorerNavigationItemConfig;
import com.microsoft.tfs.client.common.ui.teamexplorer.link.TeamExplorerBaseNavigationLink;
import com.microsoft.tfs.client.eclipse.ui.egit.Messages;
import com.microsoft.tfs.client.eclipse.ui.egit.protocolhandler.ProtocolHandlerHelpers;

public class TeamExplorerProtocolHandlerNavigationLink extends TeamExplorerBaseNavigationLink {

    private static final Log log = LogFactory.getLog(TeamExplorerProtocolHandlerNavigationLink.class);

    @Override
    public boolean isEnabled(final TeamExplorerContext context) {
        try {
            return TeamExplorerHelpers.hasProtocolHandlerRequest();
        } catch (final Exception e) {
            log.error("", e); //$NON-NLS-1$
            return false;
        }
    }

    @Override
    public boolean isVisible(final TeamExplorerContext context) {
        return isEnabled(context);
    }

    @Override
    public void onClick(
        final Shell shell,
        final TeamExplorerContext context,
        final TeamExplorerNavigator navigator,
        final TeamExplorerNavigationItemConfig parentNavigationItem) {

        if (!GitHelpers.isEGitInstalled(false)) {
            final String errorMessage =
                Messages.getString("TeamExplorerGitWizardNavigationLink.EGitMissingErrorMessageText"); //$NON-NLS-1$
            final String title = Messages.getString("TeamExplorerGitWizardNavigationLink.EGitMissingErrorMessageTitle"); //$NON-NLS-1$

            log.error("Cannot import from a Git Repository. EGit plugin is required for this action."); //$NON-NLS-1$
            MessageDialog.openError(shell, title, errorMessage);

            return;
        }

        ProtocolHandlerHelpers.clone(shell, context);
    }
}
