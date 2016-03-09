// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.views;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

import com.microsoft.tfs.client.common.autoconnect.AutoConnector;
import com.microsoft.tfs.client.common.codemarker.CodeMarker;
import com.microsoft.tfs.client.common.codemarker.CodeMarkerDispatch;
import com.microsoft.tfs.client.common.repository.RepositoryManagerAdapter;
import com.microsoft.tfs.client.common.repository.RepositoryManagerEvent;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.controls.teamexplorer.TeamExplorerControl;
import com.microsoft.tfs.client.common.ui.feedback.FeedbackAction;
import com.microsoft.tfs.client.common.ui.framework.action.ToolbarPulldownAction;
import com.microsoft.tfs.client.common.ui.framework.image.ImageHelper;
import com.microsoft.tfs.client.common.ui.productplugin.TFSProductPlugin;
import com.microsoft.tfs.client.common.ui.teamexplorer.TeamExplorerContext;
import com.microsoft.tfs.client.common.ui.teamexplorer.TeamExplorerNavigationListener;
import com.microsoft.tfs.client.common.ui.teamexplorer.TeamExplorerNavigator;
import com.microsoft.tfs.client.common.ui.teamexplorer.helpers.ConnectHelpers;
import com.microsoft.tfs.client.common.ui.teamexplorer.helpers.WebAccessHelper;
import com.microsoft.tfs.client.common.ui.teamexplorer.internal.TeamExplorerConfig;
import com.microsoft.tfs.client.common.ui.teamexplorer.internal.TeamExplorerNavigationItemConfig;
import com.microsoft.tfs.core.ConnectivityFailureStatusChangeListener;
import com.microsoft.tfs.core.clients.versioncontrol.events.WorkspaceUpdatedEvent;
import com.microsoft.tfs.core.clients.versioncontrol.events.WorkspaceUpdatedListener;

public class TeamExplorerView extends ViewPart implements ITeamExplorerView {
    public static final String ID = "com.microsoft.tfs.client.common.ui.views.TeamExplorerView"; //$NON-NLS-1$

    public static final CodeMarker FINISH_TEAM_EXPLORER_LOADING =
        new CodeMarker("com.microsoft.tfs.client.common.ui.views.TeamExplorerView#loaded"); //$NON-NLS-1$

    private final TeamExplorerConfig configuration;
    private final TeamExplorerContext context;
    private final TeamExplorerNavigator navigator;

    private final RepositoryListener repositoryListener = new RepositoryListener();
    private final NavigationListener navigationListener = new NavigationListener();

    private IAction backAction;
    private IAction forwardAction;
    final ImageHelper imageHelper = new ImageHelper(TFSCommonUIClientPlugin.PLUGIN_ID);

    private TeamExplorerControl control;

    public TeamExplorerView() {
        configuration = new TeamExplorerConfig();
        context = new TeamExplorerContext(this);
        navigator = new TeamExplorerNavigator();

        final TFSProductPlugin plugin = TFSCommonUIClientPlugin.getDefault().getProductPlugin();
        plugin.getRepositoryManager().addListener(repositoryListener);
        final AutoConnector connector = plugin.getAutoConnector();
        if (!connector.isStarted()) {
            connector.start();
        }
    }

    @Override
    public void dispose() {
        super.dispose();
        imageHelper.dispose();
        final TFSProductPlugin plugin = TFSCommonUIClientPlugin.getDefault().getProductPlugin();
        plugin.getRepositoryManager().removeListener(repositoryListener);
    }

    @Override
    public void createPartControl(final Composite parent) {
        control = new TeamExplorerControl(configuration, context, navigator, parent, SWT.NONE);

        final IActionBars bars = getViewSite().getActionBars();
        final IToolBarManager toolbarManager = bars.getToolBarManager();

        backAction = new BackAction();
        forwardAction = new ForwardAction();

        toolbarManager.add(backAction);
        toolbarManager.add(forwardAction);
        toolbarManager.add(new HomeAction());
        toolbarManager.add(new Separator());
        toolbarManager.add(new WebPortalAction());
        toolbarManager.add(new ConnectToServerAction());
        toolbarManager.add(new Separator());
        toolbarManager.add(new RefreshAction());
        toolbarManager.add(new Separator());
        toolbarManager.add(buildFeedbackAction());

        navigator.addListener(navigationListener);
        navigator.navigateToItem(configuration.getHomeNavigationItem());

        control.addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(final DisposeEvent e) {
                navigator.removeListener(navigationListener);
            }
        });

        CodeMarkerDispatch.dispatch(FINISH_TEAM_EXPLORER_LOADING);

    }

    @Override
    public TeamExplorerContext getContext() {
        return context;
    }

    @Override
    public void refresh() {
        TeamExplorerNavigationItemConfig item = navigator.getCurrentItem();
        if (item == null) {
            item = configuration.getHomeNavigationItem();
        }

        control.refreshModelAndView(item);
    }

    @Override
    public void setFocus() {
        control.setFocus();
    }

    public void navigateToPageID(final String pageID) {
        final TeamExplorerNavigationItemConfig currentItem = navigator.getCurrentItem();
        if (currentItem == null || currentItem.getTargetPageID().equals(pageID)) {
            return;
        }

        for (final TeamExplorerNavigationItemConfig configItem : configuration.getNavigationItems()) {
            if (configItem.getTargetPageID().equals(pageID)) {
                navigator.navigateToItem(configItem);
                return;
            }
        }
    }

    private final class NavigationListener implements TeamExplorerNavigationListener {
        @Override
        public void navigateToItem(TeamExplorerNavigationItemConfig item) {
            if (item == null) {
                item = configuration.getHomeNavigationItem();
            }

            control.showPageForItem(item);
        }

        @Override
        public void navigationHistoryChanged() {
            backAction.setEnabled(navigator.hasPrevious());
            forwardAction.setEnabled(navigator.hasNext());
        }
    }

    private final class RepositoryListener extends RepositoryManagerAdapter {
        private class UpdatedListener implements WorkspaceUpdatedListener {
            @Override
            public void onWorkspaceUpdated(final WorkspaceUpdatedEvent e) {
                /*
                 * Refresh the control if the workspace location changed.
                 *
                 * The original location will be null when this event comes from
                 * IPC, so don't consider that a change.
                 */
                if (e.getOriginalLocation() != null && e.getOriginalLocation() != e.getWorkspace().getLocation()) {
                    context.refresh();
                    navigator.resetNavigation(context);
                }
            }
        }

        private class CoreConnectivityFailureStatusChangeListener implements ConnectivityFailureStatusChangeListener {
            @Override
            public void onConnectivityFailureStatusChange() {
                TeamExplorerNavigationItemConfig item = navigator.getCurrentItem();
                if (item == null) {
                    item = configuration.getHomeNavigationItem();
                }

                control.refreshView(item);
            }
        }

        private final UpdatedListener updatedListener = new UpdatedListener();
        private final ConnectivityFailureStatusChangeListener connectivityListener =
            new CoreConnectivityFailureStatusChangeListener();

        @Override
        public void onRepositoryAdded(final RepositoryManagerEvent event) {
            event.getRepository().getVersionControlClient().getEventEngine().addWorkspaceUpdatedListener(
                updatedListener);
            event.getRepository().getConnection().addConnectivityFailureStatusChangeListener(connectivityListener);
        }

        @Override
        public void onRepositoryRemoved(final RepositoryManagerEvent event) {
            event.getRepository().getVersionControlClient().getEventEngine().removeWorkspaceUpdatedListener(
                updatedListener);
            event.getRepository().getConnection().removeConnectivityFailureStatusChangeListener(connectivityListener);
        }

        @Override
        public void onDefaultRepositoryChanged(final RepositoryManagerEvent event) {
            context.refresh();
            navigator.resetNavigation(context);
        }
    }

    private final class RefreshAction extends Action {
        public RefreshAction() {
            setText(Messages.getString("TeamExplorerView.RefershActionText")); //$NON-NLS-1$
            setToolTipText(Messages.getString("TeamExplorerView.RefreshActionTooltip")); //$NON-NLS-1$

            setImageDescriptor(imageHelper.getImageDescriptor("/images/common/refresh.gif")); //$NON-NLS-1$
            setDisabledImageDescriptor(imageHelper.getImageDescriptor("/images/common/refresh_disabled.gif")); //$NON-NLS-1$
        }

        @Override
        public void run() {
            refresh();
        }
    }

    private final class HomeAction extends Action {
        public HomeAction() {
            setText(Messages.getString("TeamExplorerView.HomeText")); //$NON-NLS-1$
            setToolTipText(Messages.getString("TeamExplorerView.HomeToolTip")); //$NON-NLS-1$

            final ISharedImages images = PlatformUI.getWorkbench().getSharedImages();
            setImageDescriptor(images.getImageDescriptor(ISharedImages.IMG_ETOOL_HOME_NAV));
        }

        @Override
        public void run() {
            if (navigator.getCurrentItem() != configuration.getHomeNavigationItem()) {
                navigator.navigateToItem(configuration.getHomeNavigationItem());
            }
        }
    }

    private final class WebPortalAction extends Action {
        public WebPortalAction() {
            setText(Messages.getString("TeamExplorerView.WebPortalName")); //$NON-NLS-1$
            setToolTipText(Messages.getString("TeamExplorerView.WebPortalName")); //$NON-NLS-1$
            setImageDescriptor(imageHelper.getImageDescriptor("images/common/internal_browser.gif")); //$NON-NLS-1$
        }

        @Override
        public void run() {
            WebAccessHelper.openWebAccess(context);
        }
    }

    private class ConnectToServerAction extends Action {
        public ConnectToServerAction() {
            setText(Messages.getString("TeamExplorerView.ConnectToServerText")); //$NON-NLS-1$
            setToolTipText(Messages.getString("TeamExplorerView.ConnectToServerText")); //$NON-NLS-1$
            setImageDescriptor(imageHelper.getImageDescriptor("images/common/ConnectToTfs.png")); //$NON-NLS-1$
        }

        @Override
        public void run() {
            ConnectHelpers.connectToServer(control.getShell());
        }
    }

    private final class ForwardAction extends Action {
        public ForwardAction() {
            setText(Messages.getString("TeamExplorerView.ForwardText")); //$NON-NLS-1$
            setToolTipText(Messages.getString("TeamExplorerView.ForwardToolTip")); //$NON-NLS-1$

            final ISharedImages images = PlatformUI.getWorkbench().getSharedImages();
            setImageDescriptor(images.getImageDescriptor(ISharedImages.IMG_TOOL_FORWARD));
            setDisabledImageDescriptor(images.getImageDescriptor(ISharedImages.IMG_TOOL_FORWARD_DISABLED));
        }

        @Override
        public void run() {
            navigator.navigateForward();
        }
    }

    private final class BackAction extends Action {
        public BackAction() {
            setText(Messages.getString("TeamExplorerView.BackText")); //$NON-NLS-1$
            setToolTipText(Messages.getString("TeamExplorerView.BackToolTip")); //$NON-NLS-1$

            final ISharedImages images = PlatformUI.getWorkbench().getSharedImages();
            setImageDescriptor(images.getImageDescriptor(ISharedImages.IMG_TOOL_BACK));
            setDisabledImageDescriptor(images.getImageDescriptor(ISharedImages.IMG_TOOL_BACK_DISABLED));
        }

        @Override
        public void run() {
            navigator.navigateBack();
        }
    }

    private IAction buildFeedbackAction() {
        final ToolbarPulldownAction feedbackAction = new ToolbarPulldownAction(true);
        feedbackAction.setToolTipText(Messages.getString("FeedbackButton.Tooltip")); //$NON-NLS-1$
        feedbackAction.setImageDescriptor(imageHelper.getImageDescriptor("images/common/smile.png")); //$NON-NLS-1$

        feedbackAction.addSubAction(new Action() {
            @Override
            public String getText() {
                return Messages.getString("FeedbackButton.SendSmile"); //$NON-NLS-1$
            }

            @Override
            public ImageDescriptor getImageDescriptor() {
                return imageHelper.getImageDescriptor("images/common/smile.png"); //$NON-NLS-1$
            }

            @Override
            public void run() {
                feedbackPressed(true);
            }
        });
        feedbackAction.addSubAction(new Action() {
            @Override
            public String getText() {
                return Messages.getString("FeedbackButton.SendFrown"); //$NON-NLS-1$
            }

            @Override
            public ImageDescriptor getImageDescriptor() {
                return imageHelper.getImageDescriptor("images/common/frown.png"); //$NON-NLS-1$
            }

            @Override
            public void run() {
                feedbackPressed(false);
            }
        });

        return feedbackAction;
    }

    private void feedbackPressed(final boolean smile) {
        final String viewContext = navigator.getCurrentItem().getID();
        new FeedbackAction(viewContext, smile).run(null);
    }

}
