// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.teamexplorer;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Comparator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.ToolBar;

import com.microsoft.tfs.client.common.TFSCommonClientPlugin;
import com.microsoft.tfs.client.common.repository.RepositoryManager;
import com.microsoft.tfs.client.common.repository.RepositoryManagerAdapter;
import com.microsoft.tfs.client.common.repository.RepositoryManagerEvent;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.server.TFSServer;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.framework.helper.SWTUtil;
import com.microsoft.tfs.client.common.ui.framework.helper.UIHelpers;
import com.microsoft.tfs.client.common.ui.framework.image.ImageHelper;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.teamexplorer.NewViewShowsListener;
import com.microsoft.tfs.client.common.ui.teamexplorer.ProjectAndTeamListener;
import com.microsoft.tfs.client.common.ui.teamexplorer.TeamExplorerContext;
import com.microsoft.tfs.client.common.ui.teamexplorer.TeamExplorerNavigator;
import com.microsoft.tfs.client.common.ui.teamexplorer.helpers.ConnectHelpers;
import com.microsoft.tfs.client.common.ui.teamexplorer.internal.TeamExplorerConfig;
import com.microsoft.tfs.client.common.ui.teamexplorer.internal.TeamExplorerHelpers;
import com.microsoft.tfs.client.common.ui.teamexplorer.internal.TeamExplorerNavigationItemConfig;
import com.microsoft.tfs.client.common.ui.teamexplorer.internal.TeamExplorerPageConfig;
import com.microsoft.tfs.client.common.ui.teamexplorer.internal.TeamExplorerSectionConfig;
import com.microsoft.tfs.client.common.ui.teamexplorer.items.ITeamExplorerNavigationItem;
import com.microsoft.tfs.client.common.ui.teamexplorer.pages.ITeamExplorerPage;
import com.microsoft.tfs.client.common.ui.teamexplorer.pages.TeamExplorerHomePage;
import com.microsoft.tfs.client.common.ui.teamexplorer.pages.TeamExplorerPendingChangesPage;
import com.microsoft.tfs.client.common.ui.teamexplorer.pages.TeamExplorerSettingsPage;
import com.microsoft.tfs.client.common.ui.teamexplorer.sections.ITeamExplorerSection;
import com.microsoft.tfs.core.clients.commonstructure.ProjectInfo;
import com.microsoft.tfs.core.clients.teamsettings.TeamConfiguration;
import com.microsoft.tfs.core.clients.versioncontrol.SourceControlCapabilityFlags;
import com.microsoft.tfs.core.ws.runtime.exceptions.TransportRequestHandlerCanceledException;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.tasks.CanceledException;

public class TeamExplorerControl extends TeamExplorerBaseControl {
    private static final Log log = LogFactory.getLog(TeamExplorerControl.class);

    private final TeamExplorerNavigator navigator;

    private final TeamExplorerSearchControl searchControl;

    private final StackLayout pageCompositeStackLayout;

    private final TitleBarMenuListener titleBarMenuListener = new TitleBarMenuListener();
    private final ProjectsMenuListener projectsMenuListener = new ProjectsMenuListener();
    private final RepositoryListener repositoryListener = new RepositoryListener();

    private TeamExplorerNavigationItemConfig currentNavItem;

    private UndockAction undockAction;

    private final MouseAdapter headClickAdapter;

    private final Cursor handCursor;

    public TeamExplorerControl(
        final TeamExplorerConfig configuration,
        final TeamExplorerContext context,
        final TeamExplorerNavigator navigator,
        final Composite parent,
        final int style) {
        super(configuration, context, parent, style);

        this.navigator = navigator;

        searchControl = new TeamExplorerSearchControl(form.getHead(), context, toolkit, SWT.NONE);

        pageComposite = toolkit.createComposite(subForm.getBody());
        GridDataBuilder.newInstance().fill().grab().applyTo(pageComposite);

        pageCompositeStackLayout = new StackLayout();
        pageComposite.setLayout(pageCompositeStackLayout);

        final IMenuManager menuManager = form.getMenuManager();
        menuManager.setRemoveAllWhenShown(true);
        menuManager.addMenuListener(titleBarMenuListener);
        menuManager.add(new Separator());

        handCursor = getDisplay().getSystemCursor(SWT.CURSOR_HAND);

        // Hook up a repository listener to enable/disable some controls
        final TFSCommonUIClientPlugin plugin = TFSCommonUIClientPlugin.getDefault();
        final RepositoryManager defaultRepositoryManager = plugin.getProductPlugin().getRepositoryManager();
        defaultRepositoryManager.addListener(repositoryListener);
        plugin.addProjectAndTeamListener(repositoryListener);
        plugin.addViewShowsListener(repositoryListener);

        enableSearchControl();

        final MenuManager topNavMenu = new MenuManager("#popup"); //$NON-NLS-1$
        topNavMenu.setRemoveAllWhenShown(true);
        topNavMenu.addMenuListener(titleBarMenuListener);
        topNavMenu.add(new Separator());

        final Menu menu = topNavMenu.createContextMenu(form.getHead());
        form.getHead().setMenu(menu);
        headClickAdapter = new MouseAdapter() {
            @Override
            public void mouseDown(final MouseEvent e) {
                searchControl.closePopup();
                final Control[] children = form.getHead().getChildren();
                if (children.length > 0) {
                    final Control titleArea = children[0];
                    if (titleArea != null && !titleArea.isDisposed()) {
                        menu.setLocation(titleArea.toDisplay(0, titleArea.getSize().y + 2));
                    }
                }
                menu.setVisible(true);
            }
        };
        handleHeadResize();

        addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(final DisposeEvent e) {
                defaultRepositoryManager.removeListener(repositoryListener);
                plugin.removeProjectAndTeamListener(repositoryListener);
                plugin.removeViewShowsListener(repositoryListener);
            }
        });
    }

    @Override
    public boolean setFocus() {
        form.setFocus();
        return true;
    }

    public void showPageForItem(TeamExplorerNavigationItemConfig item) {
        final TeamExplorerPageConfig page = configuration.getPage(item.getTargetPageID());
        if (page == null) {
            return;
        }

        final String viewID = item.getViewID();
        // viewID not null means this page can be undocked
        if (viewID != null && TeamExplorerHelpers.isViewUndocked(viewID)) {
            TeamExplorerHelpers.showView(viewID);
            return;
        }

        // Don't do toolkit.adapt(searchControl), as it messes up the
        // transparent background we want the control to use
        form.setHeadClient(searchControl);

        // Enable/disable the search control
        enableSearchControl();
        clearToolbar();

        // Save the current state of current page and sections before disposed.
        saveCurrentState();

        // Dispose of the currently displayed page and it's state.
        disposeCurrentPage();

        // Create the container composite for the entire page/sections content.
        final Composite fillComposite = toolkit.createComposite(pageComposite);

        // Form-style border painting not enabled (0 pixel margins OK) because
        // no applicable controls in this composite
        SWTUtil.gridLayout(fillComposite);

        // Layout the initial UI. Only show home or pending changes page when
        // offline.
        if (item == configuration.getHomeNavigationItem()
            || (!context.isConnected()
                && !TeamExplorerPendingChangesPage.ID.equals(page.getID())
                && !TeamExplorerSettingsPage.ID.equals(page.getID()))) {
            // Allocate the HOME page.
            final TeamExplorerHomePage homePage = new TeamExplorerHomePage(configuration, navigator);
            homePage.getPageContent(toolkit, fillComposite, SWT.NONE, context);
            form.setText(Messages.getString("TeamExplorerControl.Home")); //$NON-NLS-1$
            form.setMessage(getProjectAndTeamText(), IMessageProvider.NONE);
            item = configuration.getHomeNavigationItem();
        } else {
            // Layout the initial state of the page and section content. The
            // initial state may contain place holder UI for a page header or
            // sections that are to be loaded in the background. A place holder
            // UI is replaced when its associated background processing ends and
            // the real UI can be generated.

            if (page.getID().equals(TeamExplorerPendingChangesPage.ID)) {
                TFSCommonUIClientPlugin.getDefault().getPendingChangesViewModel().initialize();
            }

            createPageContent(fillComposite, page, configuration.getPageSections(page.getID()));
            form.setText(page.getTitle());
            form.setMessage(getProjectAndTeamText(), IMessageProvider.NONE);

            if (viewID != null) {
                addUndockButton(form.getHead());
            }
        }
        hookLeftClick();

        // Make the newly laid out UI visible.
        pageCompositeStackLayout.topControl = fillComposite;
        handleBodyResize();

        // Update the check mark on the title's context menu.
        currentNavItem = item;

        ConnectHelpers.showHideViews(context.getSourceControlCapability());
    }

    private void addUndockButton(final Composite composite) {
        if (undockAction == null) {
            undockAction = new UndockAction();
        }

        form.getToolBarManager().add(undockAction);
        form.getToolBarManager().update(true);
        handleHeadResize();
    }

    private void clearToolbar() {
        form.getToolBarManager().removeAll();
        handleHeadResize();
    }

    protected void showPreviousPage() {
        navigator.navigateOut(currentNavItem);
    }

    private void undockCurrentPage() {
        final String viewID = currentNavItem.getViewID();
        if (viewID != null) {
            TeamExplorerHelpers.showView(viewID);
        }
    }

    private void hookLeftClick() {
        final Control[] headControls = form.getHead().getChildren();
        for (final Control control : headControls) {
            if (control instanceof Canvas && !(control instanceof ToolBar)) {
                control.addMouseListener(headClickAdapter);
                control.setCursor(handCursor);
                final Control[] subcontrols = ((Composite) control).getChildren();
                for (final Control subcontrol : subcontrols) {
                    subcontrol.addMouseListener(headClickAdapter);
                }
            }
        }
    }

    private void unhookLeftClick() {
        final Control[] headControls = form.getHead().getChildren();
        for (final Control control : headControls) {
            if (control instanceof Canvas && !(control instanceof ToolBar)) {
                control.removeMouseListener(headClickAdapter);
                final Control[] subcontrols = ((Composite) control).getChildren();
                for (final Control subcontrol : subcontrols) {
                    subcontrol.removeMouseListener(headClickAdapter);
                }
            }
        }
    }

    public void refreshModelAndView(final TeamExplorerNavigationItemConfig item) {
        // Show the refresh UI in team explorer.
        UIHelpers.runOnUIThread(true, new Runnable() {
            @Override
            public void run() {
                // Create the target page configuration and create a page
                // instance.
                final ITeamExplorerPage page;
                if (item.equals(configuration.getHomeNavigationItem())) {
                    page = new TeamExplorerHomePage(configuration, navigator);
                } else {
                    final TeamExplorerPageConfig pageConfig = configuration.getPage(item.getTargetPageID());
                    page = pageConfig.createInstance();
                }

                // Create a background job to perform the refresh. The job
                // change
                // listener will refresh the view when the job completes.
                final PageRefreshJob job = new PageRefreshJob(item.getTitle(), page);
                job.setProperty(NAVITEM_CONFIG_DATA_NAME, item);
                job.addJobChangeListener(new PageRefreshJobChangeListener());
                job.schedule();
            }
        });
    }

    public void refreshView(final TeamExplorerNavigationItemConfig item) {
        UIHelpers.runOnUIThread(true, new Runnable() {
            @Override
            public void run() {
                showPageForItem(item);
            }
        });
    }

    @Override
    protected Composite getParentComposite() {
        return (Composite) pageComposite.getChildren()[0];
    }

    /**
     * Save the current state of current page and sections before dispose
     */
    private void saveCurrentState() {
        if (currentPage != null && pageComposite.getChildren().length > 0) {
            final TeamExplorerPageConfig page = configuration.getPage(currentNavItem.getTargetPageID());

            if (page != null) {
                final Object pageState = currentPage.saveState();
                if (pageState != null) {
                    stateMap.put(page.getID(), pageState);
                }
                final TeamExplorerSectionConfig[] sections = configuration.getPageSections(page.getID());
                if (sections != null) {
                    for (final TeamExplorerSectionConfig section : sections) {
                        if (teamExplorerSectionMap.get(section.getID()) != null) {
                            final ITeamExplorerSection sectionInstance = teamExplorerSectionMap.get(section.getID());
                            sectionExpandStateMap.put(section.getID(), sectionMap.get(section.getID()).isExpanded());
                            final Object sectionState = sectionInstance.saveState();
                            if (sectionState != null) {
                                stateMap.put(section.getID(), sectionState);
                            }
                        }
                    }
                }
            }
            teamExplorerSectionMap.clear();
            currentPage = null;
        }
    }

    private void disposeCurrentPage() {
        unhookLeftClick();
        if (pageComposite.getChildren().length > 0) {
            pageComposite.getChildren()[0].dispose();
        }
    }

    /**
     * Sets the search control's enabled state depending on whether there is an
     * active repository. For a server workspace, the control will be disabled
     * when "offline", enabled when "online". For a local workspace, it remains
     * enabled all the time (even when the last web service had a connectivity
     * problem).
     */
    private void enableSearchControl() {
        final TFSRepository defaultRepository =
            TFSCommonUIClientPlugin.getDefault().getProductPlugin().getRepositoryManager().getDefaultRepository();

        searchControl.setEnabled(defaultRepository != null && context.getCurrentProjectInfo() != null);
    }

    private class PageRefreshJob extends Job {
        private final ITeamExplorerPage pageInstance;

        public PageRefreshJob(final String title, final ITeamExplorerPage pageInstance) {
            super(title);
            this.pageInstance = pageInstance;
        }

        @Override
        protected IStatus run(final IProgressMonitor monitor) {
            try {
                if (pageInstance != null) {
                    pageInstance.refresh(monitor, context);
                }
            } catch (final CanceledException e) {
                return Status.CANCEL_STATUS;
            } catch (final TransportRequestHandlerCanceledException e) {
                return Status.CANCEL_STATUS;
            } catch (final Exception e) {
                log.error("Failed to refresh Team Explorer page", e); //$NON-NLS-1$

                // Return a status of WARNING so that the progress monitor won't
                // display a modal dialog for an ERROR status. We'll handle the
                // exception in the job adapter's 'done' method.
                return new Status(Status.WARNING, TFSCommonClientPlugin.PLUGIN_ID, null, e);
            }

            return monitor.isCanceled() ? Status.CANCEL_STATUS : Status.OK_STATUS;
        }
    }

    private class PageRefreshJobChangeListener extends JobChangeAdapter {
        @Override
        public void done(final IJobChangeEvent event) {
            final TeamExplorerNavigationItemConfig item =
                (TeamExplorerNavigationItemConfig) event.getJob().getProperty(NAVITEM_CONFIG_DATA_NAME);

            refreshView(item);
        }
    }

    private class TitleBarMenuListener implements IMenuListener {
        @Override
        public void menuAboutToShow(final IMenuManager menuManager) {
            // add the navigate to HOME page command.
            final TeamExplorerNavigationItemConfig homeNavItem = configuration.getHomeNavigationItem();

            final Action homeAction = new Action(configuration.getHomeNavigationItem().getTitle()) {
                @Override
                public void run() {
                    navigator.navigateToItem(configuration.getHomeNavigationItem());
                }
            };

            homeAction.setId(homeNavItem.getID());
            menuManager.add(homeAction);

            // Add navigate commands for each visible page except Settings (it
            // goes at the end)
            for (final TeamExplorerNavigationItemConfig navItem : configuration.getNavigationItems()) {
                // Determine if the navigation item is visible in the current
                // context and and create an action item if it is.
                final ITeamExplorerNavigationItem instance = navItem.createInstance();
                if (instance.isVisible(context)) {
                    final Action action = new Action(navItem.getTitle()) {
                        @Override
                        public void run() {
                            final String viewID = navItem.getViewID();

                            // do specific if targetPageID is null
                            if (navItem.getTargetPageID() == null) {
                                instance.clicked(context);
                            }
                            // viewID not null -> check undocked views
                            else if (viewID != null && TeamExplorerHelpers.isViewUndocked(viewID)) {
                                TeamExplorerHelpers.showView(viewID);
                            }
                            // other cases -> navigate in Team Explorer view
                            else {
                                navigator.navigateToItem(navItem);
                            }
                        }
                    };

                    /*
                     * The navigation item ID is used to determine which action
                     * matches the currently shown navigation item so that the
                     * menu listener can properly set the check mark on the
                     * currently displayed navigation item.
                     */
                    action.setId(navItem.getID());

                    // Add the action to the context menu.
                    menuManager.add(action);
                }
            }

            menuManager.add(new Separator());

            // create the project/team fly out menu. "Connect to server" is
            // always
            // present so the menu won't be empty and omitted when disconnected.
            String subMenuTitle;
            if (TeamExplorerHelpers.supportsTeam(context)) {
                subMenuTitle = Messages.getString("TeamExplorerControl.ProjectsAndMyTeamsSubMenuText"); //$NON-NLS-1$
            } else {
                subMenuTitle = Messages.getString("TeamExplorerControl.ProjectsSubMenuText"); //$NON-NLS-1$
            }

            final IMenuManager subMenu = new MenuManager(subMenuTitle);
            subMenu.setRemoveAllWhenShown(true);
            subMenu.addMenuListener(projectsMenuListener);
            subMenu.add(new ConnectToServerAction());
            menuManager.add(subMenu);

            setCheckedItem(menuManager);
        }

        private void setCheckedItem(final IMenuManager menuManager) {
            for (final IContributionItem item : menuManager.getItems()) {
                if (item instanceof ActionContributionItem) {
                    /*
                     * Actions at the top level are navigation actions. Set the
                     * check mark for the action associated with the current
                     * navigation item. Clear the check mark for all others.
                     */
                    final ActionContributionItem actionContributionItem = (ActionContributionItem) item;
                    final String actionID = actionContributionItem.getAction().getId();

                    actionContributionItem.getAction().setChecked(
                        actionID != null && actionID.equals(currentNavItem.getID()));
                }
            }
        }
    }

    private class ProjectsMenuListener implements IMenuListener {
        @Override
        public void menuAboutToShow(final IMenuManager menuManager) {
            if (!context.isConnected()) {
                menuManager.add(new ConnectToServerAction());
                return;
            }

            addProjectsAndTeams(menuManager);

            if (TeamExplorerHelpers.supportsTeam(context)) {
                menuManager.add(new Separator());
                menuManager.add(new RefreshTeamsAction());
            }

            menuManager.add(new Separator());
            menuManager.add(new ConnectToServerAction());
        }

        private void addProjectsAndTeams(final IMenuManager menuManager) {
            final ProjectInfo[] projects = context.getServer().getProjectCache().getActiveTeamProjects();

            // Sort projects alphabetically
            Arrays.sort(projects, ProjectNameComparator.INSTANCE);

            if (projects != null && projects.length >= 0) {
                for (final ProjectInfo project : projects) {
                    final TeamConfiguration[] teams = context.getServer().getProjectCache().getTeams(project);

                    if (teams.length == 0) {
                        // Server does not support TFS 2012 teams or for some
                        // reason the default team got deleted (should not
                        // happen)

                        menuManager.add(createAction(project));
                    } else {
                        // Sort teams alphabetically
                        Arrays.sort(teams, TeamNameComparator.INSTANCE);

                        for (final TeamConfiguration team : teams) {
                            menuManager.add(createAction(project, team));
                        }
                    }
                }
            }
        }

        /**
         * Creates a menu action for a team project when TFS 2012 teams are not
         * available.
         */
        private Action createAction(final ProjectInfo project) {
            Check.notNull(project, "project"); //$NON-NLS-1$

            final String projectGUID = project.getGUID();

            final Action action = new Action(project.getName()) {
                @Override
                public void run() {
                    if (!context.getCurrentProjectInfo().getGUID().equals(projectGUID)) {
                        context.setCurrentProject(projectGUID);
                        context.setCurrentTeam(null);
                        final boolean tfvc = context.getCurrentProjectInfo().getSourceControlCapabilityFlags().contains(
                            SourceControlCapabilityFlags.TFS);
                        TFSCommonUIClientPlugin.getDefault().projectOrTeamChanged();
                        TFSCommonUIClientPlugin.getDefault().sourceControlChanged(tfvc);
                    }
                }
            };

            if (context.getCurrentProjectInfo().getGUID().equals(projectGUID)) {
                action.setChecked(true);
            }

            return action;
        }

        /**
         * Creates a menu action for a team project and team. The team name is
         * omitted if it is the default team for that project.
         */
        private Action createAction(final ProjectInfo project, final TeamConfiguration team) {
            Check.notNull(project, "project"); //$NON-NLS-1$
            Check.notNull(team, "team"); //$NON-NLS-1$

            final String projectGUID = project.getGUID();

            // Omit the team name for the default team
            final String actionName = team.isDefaultTeam() ? project.getName()
                : MessageFormat.format(
                    Messages.getString("TeamExplorerControl.ProjectSlashTeamFormat"), //$NON-NLS-1$
                    project.getName(),
                    team.getTeamName());

            final Action action = new Action(actionName) {
                @Override
                public void run() {
                    final String beforeChangeProjectGUID = context.getCurrentProjectInfo().getGUID();
                    if (!projectGUID.equals(beforeChangeProjectGUID) || !team.equals(context.getCurrentTeam())) {
                        context.setCurrentProject(projectGUID);
                        context.setCurrentTeam(team);

                        TFSCommonUIClientPlugin.getDefault().projectOrTeamChanged();

                        // Only invoke this listener if team project changed
                        if (!projectGUID.equals(beforeChangeProjectGUID)) {
                            final boolean tfvc =
                                context.getCurrentProjectInfo().getSourceControlCapabilityFlags().contains(
                                    SourceControlCapabilityFlags.TFS);
                            TFSCommonUIClientPlugin.getDefault().sourceControlChanged(tfvc);
                        }
                    }
                }
            };

            if (projectGUID.equals(context.getCurrentProjectInfo().getGUID())
                && team.equals(context.getCurrentTeam())) {
                action.setChecked(true);
            }

            return action;
        }
    }

    private class RefreshTeamsAction extends Action {
        @Override
        public String getText() {
            return Messages.getString("TeamExplorerControl.RefreshTeamsCommandText"); //$NON-NLS-1$
        }

        @Override
        public void run() {
            setChecked(false); // never show the context menu check

            final TFSServer server = context.getServer();
            if (server != null) {
                // Refreshes available projects and teams
                server.getProjectCache().refresh();
            }
        }
    }

    private class ConnectToServerAction extends Action {
        @Override
        public String getText() {
            return Messages.getString("TeamExplorerControl.ConnectCommandText"); //$NON-NLS-1$
        }

        @Override
        public void run() {
            setChecked(false); // never show the context menu check
            ConnectHelpers.connectToServer(getShell());
        }
    }

    private final class UndockAction extends Action {
        public UndockAction() {
            setText(Messages.getString("TeamExplorerControl.UndockText")); //$NON-NLS-1$
            setToolTipText(Messages.getString("TeamExplorerControl.UndockToolTipText")); //$NON-NLS-1$

            final ImageHelper imageHelper = new ImageHelper(TFSCommonUIClientPlugin.PLUGIN_ID);
            setImageDescriptor(imageHelper.getImageDescriptor("/images/common/undock.png")); //$NON-NLS-1$
        }

        @Override
        public void run() {
            showPreviousPage();
            undockCurrentPage();
        }
    }

    private static class ProjectNameComparator implements Comparator<ProjectInfo> {
        public static final ProjectNameComparator INSTANCE = new ProjectNameComparator();

        @Override
        public int compare(final ProjectInfo o1, final ProjectInfo o2) {
            return String.CASE_INSENSITIVE_ORDER.compare(o1.getName(), o2.getName());
        }
    }

    private static class TeamNameComparator implements Comparator<TeamConfiguration> {
        public static final TeamNameComparator INSTANCE = new TeamNameComparator();

        @Override
        public int compare(final TeamConfiguration o1, final TeamConfiguration o2) {
            return String.CASE_INSENSITIVE_ORDER.compare(o1.getTeamName(), o2.getTeamName());
        }
    }

    private class RepositoryListener extends RepositoryManagerAdapter
        implements ProjectAndTeamListener, NewViewShowsListener {
        @Override
        public void onDefaultRepositoryChanged(final RepositoryManagerEvent event) {
            UIHelpers.runOnUIThread(true, new Runnable() {
                @Override
                public void run() {
                    if (searchControl != null
                        && searchControl.getDisplay() != null
                        && !searchControl.getDisplay().isDisposed()) {
                        searchControl.clear();
                        enableSearchControl();
                    }
                }
            });
        }

        @Override
        public void onProjectOrTeamChanged() {
            ConnectHelpers.showHideViews(context.getSourceControlCapability());

            navigator.resetNavigation(context);
        }

        @Override
        public void onNewViewShows(final String pageId) {
            if (currentNavItem != null && pageId.equals(currentNavItem.getTargetPageID())) {
                showPreviousPage();
                undockCurrentPage();
            }
        }
    }
}
