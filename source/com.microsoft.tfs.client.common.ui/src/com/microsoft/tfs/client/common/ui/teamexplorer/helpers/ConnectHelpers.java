// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teamexplorer.helpers;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.activities.IActivityManager;
import org.eclipse.ui.activities.IWorkbenchActivitySupport;

import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.server.TFSServer;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.browser.BrowserFacade;
import com.microsoft.tfs.client.common.ui.browser.BrowserFacade.LaunchMode;
import com.microsoft.tfs.client.common.ui.productplugin.TFSProductPlugin;
import com.microsoft.tfs.client.common.ui.wizard.teamprojectwizard.ITeamProjectWizard;
import com.microsoft.tfs.core.clients.versioncontrol.SourceControlCapabilityFlags;

public class ConnectHelpers {
    public static void connectToServer(final Shell shell) {
        final TFSProductPlugin plugin = TFSCommonUIClientPlugin.getDefault().getProductPlugin();
        final ITeamProjectWizard wizard = plugin.getTeamProjectWizard();
        final WizardDialog dialog = new WizardDialog(shell, wizard);

        if (dialog.open() == IDialogConstants.OK_ID) {
            final TFSServer defaultServer = plugin.getServerManager().getDefaultServer();
            final TFSRepository[] repositories = plugin.getRepositoryManager().getRepositories();

            if (defaultServer != null) {
                defaultServer.refresh(true);
            }

            for (int i = 0; i < repositories.length; i++) {
                repositories[i].getPendingChangeCache().refresh();
            }
        }
    }

    public static void signupForTeamFoundationService(final Shell shell) {
        try {
            // Use external dialog since the signup process may take a while,
            // involve research in more tabs, etc.
            BrowserFacade.launchURL(
                new URI("http://go.microsoft.com/fwlink/?LinkId=613630"), //$NON-NLS-1$
                null,
                null,
                null,
                LaunchMode.EXTERNAL);
        } catch (final URISyntaxException e) {
        }
    }

    /**
     * Show / Hide views according to the current connected team project In case
     * there is no current team project, all views are visible
     *
     * @param flags
     */
    public static void showHideViews(final SourceControlCapabilityFlags flags) {
        final IWorkbenchActivitySupport workbenchActivitySupport = PlatformUI.getWorkbench().getActivitySupport();
        final IActivityManager activityManager = workbenchActivitySupport.getActivityManager();
        final Set enabledActivityIds = new HashSet(activityManager.getEnabledActivityIds());

        if (flags != null && flags.contains(SourceControlCapabilityFlags.GIT)) {
            if (enabledActivityIds.remove("com.microsoft.tfs.git.hidden")) //$NON-NLS-1$
            {
                workbenchActivitySupport.setEnabledActivityIds(enabledActivityIds);
            }
        } else {
            if (enabledActivityIds.add("com.microsoft.tfs.git.hidden")) //$NON-NLS-1$
            {
                workbenchActivitySupport.setEnabledActivityIds(enabledActivityIds);
            }
        }
    }
}
