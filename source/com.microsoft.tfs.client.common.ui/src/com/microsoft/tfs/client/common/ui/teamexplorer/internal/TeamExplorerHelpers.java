// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teamexplorer.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.forms.widgets.SharedScrolledComposite;

import com.microsoft.tfs.client.common.server.TFSServer;
import com.microsoft.tfs.client.common.ui.framework.telemetry.ClientTelemetryHelper;
import com.microsoft.tfs.client.common.ui.teamexplorer.TeamExplorerContext;
import com.microsoft.tfs.client.common.ui.views.TeamExplorerPendingChangesView;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.teamstore.TeamProjectCollectionTeamStore;
import com.microsoft.tfs.core.clients.versioncontrol.SourceControlCapabilityFlags;
import com.microsoft.tfs.core.clients.versioncontrol.WebServiceLevel;
import com.microsoft.tfs.core.clients.webservices.IIdentityManagementService2;
import com.microsoft.tfs.core.clients.webservices.IdentityManagementException;
import com.microsoft.tfs.core.clients.webservices.IdentityManagementService2;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.Platform;

/**
 *
 */
public class TeamExplorerHelpers {
    private static final Log log = LogFactory.getLog(TeamExplorerHelpers.class);

    public static final int MOUSE_RIGHT_BUTTON = 3;

    public static final String PendingChangesViewID = TeamExplorerPendingChangesView.ID;
    public static final String BuildsViewID =
        "com.microsoft.tfs.client.common.ui.teambuild.teamexplorer.views.TeamExplorerBuildView"; //$NON-NLS-1$

    public static final String PendingChangeNavItemID =
        "com.microsoft.tfs.client.common.ui.teamexplorer.items.TeamExplorerPendingChangesNavigationItem"; //$NON-NLS-1$
    public static final String VersionControlNavItemID =
        "com.microsoft.tfs.client.common.ui.vcexplorer.teamexplorer.TeamExplorerVersionControlNavigationItem"; //$NON-NLS-1$
    public static final String WorkItemNavItemID =
        "com.microsoft.tfs.client.common.ui.teamexplorer.items.TeamExplorerWorkItemsNavigationItem"; //$NON-NLS-1$
    public static final String BuildNavItemID =
        "com.microsoft.tfs.client.common.ui.teambuild.teamexplorer.items.TeamExplorerBuildNavigationItem"; //$NON-NLS-1$
    public static final String DocumentsNavItemID =
        "com.microsoft.tfs.client.common.ui.teamexplorer.items.TeamExplorerDocumentsNavigationItem"; //$NON-NLS-1$
    public static final String ReportsNavItemID =
        "com.microsoft.tfs.client.common.ui.teamexplorer.items.TeamExplorerReportsNavigationItem"; //$NON-NLS-1$
    public static final String SettingsNavItemID =
        "com.microsoft.tfs.client.common.ui.teamexplorer.items.TeamExplorerSettingsNavigationItem"; //$NON-NLS-1$

    public static final String EGitRepoViewID = "org.eclipse.egit.ui.RepositoriesView"; //$NON-NLS-1$

    public static void relayoutContainingScrolledComposite(Composite composite) {
        SharedScrolledComposite scrolledComposite = null;
        while (composite != null) {
            if (composite instanceof SharedScrolledComposite) {
                scrolledComposite = (SharedScrolledComposite) composite;
                break;
            }

            composite = composite.getParent();
        }

        Check.notNull(scrolledComposite, "scrolledComposite"); //$NON-NLS-1$

        scrolledComposite.layout(true, true);
        scrolledComposite.reflow(true);
    }

    public static void relayoutIfResized(final Composite composite) {
        final Point currentSize = composite.getSize();
        final Point newSize = composite.computeSize(SWT.DEFAULT, SWT.DEFAULT);

        if (!currentSize.equals(newSize)) {
            TeamExplorerHelpers.relayoutContainingScrolledComposite(composite);
        }
    }

    public static void showOrHideSection(final Composite composite, final boolean show) {
        final Section section = getContainingSection(composite);

        if (section != null) {
            final GridData gridData = (GridData) section.getLayoutData();
            gridData.exclude = !show;
            section.setVisible(show);
        }
    }

    public static void updateContainingSectionTitle(final Composite composite, final String title) {
        final Section section = getContainingSection(composite);

        if (section != null) {
            section.setText(title);
        }
    }

    public static void toggleContainingSection(final Composite composite) {
        final Section section = getContainingSection(composite);

        if (section != null) {
            section.setExpanded(false);
            section.setExpanded(true);
        }
    }

    public static void toggleCompositeVisibility(final Composite composite) {
        Check.isTrue(composite.getLayoutData() instanceof GridData, "composite.getLayoutData() instanceof GridData"); //$NON-NLS-1$

        final GridData gridData = (GridData) composite.getLayoutData();

        if (gridData.exclude) {
            gridData.exclude = false;
            composite.setVisible(true);
        } else {
            gridData.exclude = true;
            composite.setVisible(false);
        }

        // reflowContainingScrolledComposite(composite);
    }

    public static Color getDropCompositeForeground(final Composite parent) {
        if (Platform.isCurrentPlatform(Platform.WINDOWS)) {
            return parent.getDisplay().getSystemColor(SWT.COLOR_TITLE_FOREGROUND);
        } else {
            return parent.getDisplay().getSystemColor(SWT.COLOR_WIDGET_FOREGROUND);
        }
    }

    public static Color getDropCompositeBackground(final Composite parent) {
        if (Platform.isCurrentPlatform(Platform.WINDOWS)) {
            return parent.getDisplay().getSystemColor(SWT.COLOR_TITLE_BACKGROUND);
        } else {
            return parent.getDisplay().getSystemColor(SWT.COLOR_TITLE_INACTIVE_BACKGROUND);
        }
    }

    /**
     * @return true if we're connected to a server that supports teams
     *         (introduced in TFS 2012), false if we're not connected or the
     *         server does not support teams
     */
    public static boolean supportsTeam(final TeamExplorerContext context) {
        /*
         * Avoid using the ServerProjectCache if we're not connected, because it
         * would bring us online or throw exceptions if the server is
         * unreachable.
         */
        if (!context.isConnectedToCollection()) {
            return false;
        }

        final TFSServer server = context.getServer();

        if (server == null) {
            return false;
        }

        if (context.isConnected()) {
            return server.getProjectCache().supportsTeam();
        } else {
            return ((TeamProjectCollectionTeamStore) server.getConnection().getClient(
                TeamProjectCollectionTeamStore.class)).supportsTeam();
        }
    }

    public static boolean supportsGit(final TeamExplorerContext context) {
        return supportsSourceControlFlag(context, SourceControlCapabilityFlags.GIT);
    }

    public static boolean supportsTfvc(final TeamExplorerContext context) {
        return supportsSourceControlFlag(context, SourceControlCapabilityFlags.TFS);
    }

    private static boolean supportsSourceControlFlag(
        final TeamExplorerContext context,
        final SourceControlCapabilityFlags flag) {
        if (!context.isConnected()) {
            return false;
        }

        final SourceControlCapabilityFlags flags = context.getSourceControlCapability();
        return flags.contains(flag);
    }

    /**
     * @return true if we're connected to a server that supports
     *         IdentityService2 (introduced in TFS 2010), false if we're not
     *         connected or the server does not support teams
     */
    public static boolean supportsIdentityService2(final TFSTeamProjectCollection tpc) {
        try {
            return ((IdentityManagementService2) tpc.getClient(IIdentityManagementService2.class)).isSupported();
        } catch (final IdentityManagementException e) {
            return false;
        }
    }

    public static boolean supportsMyFavorites(final TFSTeamProjectCollection tpc) {
        if (tpc == null) {
            return false;
        }

        return supportsIdentityService2(tpc);
    }

    public static boolean supportsTeamFavorites(final TFSTeamProjectCollection tpc) {
        if (tpc == null) {
            return false;
        }

        return ((TeamProjectCollectionTeamStore) tpc.getClient(TeamProjectCollectionTeamStore.class)).supportsTeam();
    }

    public static boolean isVersion2010OrGreater(final TeamExplorerContext context) {
        if (!context.isConnectedToCollection()) {
            return false;
        }

        final WebServiceLevel level = context.getServer().getConnection().getVersionControlClient().getServiceLevel();
        return level.getValue() >= WebServiceLevel.TFS_2010.getValue();
    }

    public static boolean isVersion2012OrGreater(final TeamExplorerContext context) {
        if (!context.isConnected()) {
            return false;
        }

        final WebServiceLevel level = context.getServer().getConnection().getVersionControlClient().getServiceLevel();
        return level.getValue() >= WebServiceLevel.TFS_2012_2.getValue();
    }

    private static Section getContainingSection(Composite composite) {
        Section section = null;
        while (composite != null) {
            if (composite instanceof Section) {
                section = (Section) composite;
                break;
            }

            composite = composite.getParent();
        }

        return section;
    }

    /**
     * Helper method to check whether a TE view is open
     *
     * @param viewID
     * @return
     */
    public static boolean isViewUndocked(final String viewID) {
        final IViewPart view = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().findView(viewID);

        return view != null;
    }

    /**
     * Helper method to show a Team Explorer view based on viewID
     *
     * @param viewID
     */
    public static IViewPart showView(final String viewID) {
        ClientTelemetryHelper.sendTeamExplorerPageView(viewID);

        IViewPart view = null;
        try {
            view = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView(viewID);
        } catch (final PartInitException e) {
            log.error(e);
        }

        return view;
    }
}
