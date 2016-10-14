// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.ui.egit.teamexplorer;

import java.util.Arrays;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

import com.microsoft.tfs.client.common.git.utils.GitHelpers;
import com.microsoft.tfs.client.common.ui.protocolhandler.ProtocolHandler;
import com.microsoft.tfs.client.common.ui.teamexplorer.TeamExplorerContext;
import com.microsoft.tfs.client.common.ui.teamexplorer.TeamExplorerNavigator;
import com.microsoft.tfs.client.common.ui.teamexplorer.internal.TeamExplorerNavigationItemConfig;
import com.microsoft.tfs.client.common.ui.teamexplorer.link.TeamExplorerBaseNavigationLink;
import com.microsoft.tfs.client.common.ui.vc.serveritem.TypedServerGitRepository;
import com.microsoft.tfs.client.common.ui.vc.serveritem.TypedServerItem;
import com.microsoft.tfs.client.eclipse.ui.egit.Messages;
import com.microsoft.tfs.client.eclipse.ui.egit.importwizard.GitImportWizard;
import com.microsoft.tfs.client.eclipse.ui.egit.importwizard.WizardCrossCollectionRepoSelectionPage;
import com.microsoft.tfs.client.eclipse.ui.egit.protocolhandler.ProtocolHandlerHelpers;

public class TeamExplorerProtocolHandlerNavigationLink extends TeamExplorerBaseNavigationLink {

    private static final Log log = LogFactory.getLog(TeamExplorerProtocolHandlerNavigationLink.class);

    @Override
    public boolean isEnabled(final TeamExplorerContext context) {
        try {
            return ProtocolHandler.getInstance().hasProtocolHandlerRequest();
        } catch (final Exception e) {
            log.error("", e); //$NON-NLS-1$
            return false;
        }
    }

    @Override
    public boolean isVisible(final TeamExplorerContext context) {
        return isEnabled(context) && context.isConnectedToCollection();
    }

    @Override
    public void onClick(
        final Shell shell,
        final TeamExplorerContext context,
        final TeamExplorerNavigator navigator,
        final TeamExplorerNavigationItemConfig parentNavigationItem) {

        // At this point we need EGit/JGit installed at activated
        if (!GitHelpers.isEGitInstalled(true)) {
            final String errorMessage =
                Messages.getString("TeamExplorerGitWizardNavigationLink.EGitMissingErrorMessageText"); //$NON-NLS-1$
            final String title = Messages.getString("TeamExplorerGitWizardNavigationLink.EGitMissingErrorMessageTitle"); //$NON-NLS-1$

            log.error("Cannot import from a Git Repository. EGit plugin is required for this action."); //$NON-NLS-1$
            MessageDialog.openError(shell, title, errorMessage);

            return;
        }

        final ProtocolHandlerHelpers handler = new ProtocolHandlerHelpers(context);
        final TypedServerGitRepository typedRepo = handler.getImportWizardInput();

        final GitImportWizard wizard = new GitImportWizard(Arrays.asList(new TypedServerItem[] {
            typedRepo
        }));

        wizard.init(PlatformUI.getWorkbench(), null);
        wizard.setPageData(WizardCrossCollectionRepoSelectionPage.INITIALLY_SELECTED_REPO, typedRepo);
        wizard.setPageData(WizardCrossCollectionRepoSelectionPage.PROTOCOL_HANDLER_REPO, typedRepo);

        final WizardDialog dialog = new WizardDialog(shell, wizard);
        dialog.open();
    }
}
