// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teambuild.editors;

import java.text.MessageFormat;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import com.microsoft.tfs.client.common.server.ServerManagerAdapter;
import com.microsoft.tfs.client.common.server.ServerManagerEvent;
import com.microsoft.tfs.client.common.server.ServerManagerListener;
import com.microsoft.tfs.client.common.server.TFSServer;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.editors.ConnectionSpecificPart;
import com.microsoft.tfs.client.common.ui.framework.helper.UIHelpers;
import com.microsoft.tfs.client.common.ui.teambuild.Messages;
import com.microsoft.tfs.client.common.ui.teambuild.TFSTeamBuildPlugin;
import com.microsoft.tfs.client.common.ui.teambuild.actions.RefreshBuildExplorerAction;
import com.microsoft.tfs.core.clients.build.IBuildDefinition;
import com.microsoft.tfs.core.clients.build.IBuildDetail;
import com.microsoft.tfs.core.clients.build.IBuildServer;
import com.microsoft.tfs.core.clients.build.IQueuedBuild;
import com.microsoft.tfs.core.clients.build.utils.BuildPath;

public class BuildExplorer extends MultiPageEditorPart implements ConnectionSpecificPart {
    public static final String ID = "com.microsoft.tfs.client.common.ui.teambuild.editors.BuildExplorer"; //$NON-NLS-1$

    private IBuildDefinition buildDefinition;

    private BuildEditorPage buildEditorPage;
    private QueueEditorPage queueEditorPage;
    private int buildPage = -1;
    private int queuePage = -1;
    private final Image completedPageImage;
    private final Image queuedPageImage;
    private IBuildServer buildServer;
    private Job pollJob;
    private int pollInterval;
    private final RefreshBuildExplorerAction refreshAction;
    private boolean disposed = false;
    private boolean firstTimeIn = true;
    private ServerManagerListener serverManagerListener;
    private boolean isConnected = true;

    private static BuildExplorer instance;

    public BuildExplorer() {
        super();
        queuedPageImage = AbstractUIPlugin.imageDescriptorFromPlugin(
            TFSTeamBuildPlugin.PLUGIN_ID,
            "/icons/BuildStatusNotStarted.gif").createImage(); //$NON-NLS-1$
        completedPageImage = AbstractUIPlugin.imageDescriptorFromPlugin(
            TFSTeamBuildPlugin.PLUGIN_ID,
            "/icons/BuildStatusPartial.gif").createImage(); //$NON-NLS-1$

        refreshAction = new RefreshBuildExplorerAction();
        instance = this;
    }

    /**
     * @see org.eclipse.ui.part.MultiPageEditorPart#init(org.eclipse.ui.IEditorSite,
     *      org.eclipse.ui.IEditorInput)
     */
    @Override
    public void init(final IEditorSite site, final IEditorInput input) throws PartInitException {
        if (!(input instanceof BuildExplorerEditorInput)) {
            throw new PartInitException("Invalid Input: Must be BuildExplorerEditorInput"); //$NON-NLS-1$
        }

        buildDefinition = ((BuildExplorerEditorInput) input).getBuildDefinition();
        buildServer = buildDefinition.getBuildServer();

        pollInterval = 30000;

        refreshAction.setActiveEditor(this);

        super.init(site, input);
    }

    public void setBuildDefinition(final IBuildDefinition buildDefinition) {
        if (!firstTimeIn && this.buildDefinition != null && !this.buildDefinition.equals(buildDefinition)) {
            // Not the first time in, therefore we are re-using an editor.
            this.buildDefinition = buildDefinition;
            buildServer = this.buildDefinition.getBuildServer();

            buildEditorPage.setSelectedBuildDefinition(this.buildDefinition, true);

            if (queueEditorPage != null) {
                queueEditorPage.setSelectedBuildDefinition(this.buildDefinition, true);
            }

            setName(this.buildDefinition);
        }
        updatePageResults(getActivePage());
        firstTimeIn = false;
    }

    public void setSelectedQueuedBuild(final IQueuedBuild queuedBuild) {
        queueEditorPage.setSelectedQueuedBuild(queuedBuild);
        setActivePage(queuePage);
        updatePageResults(queuePage);
    }

    public void activateBuildPage() {
        setActivePage(buildPage);
        updatePageResults(buildPage);
    }

    public void activateQueuedPage() {
        setActivePage(queuePage);
        updatePageResults(queuePage);
    }

    public void showManageQueueView() {
        queueEditorPage.setManageQueueFilters();
        activateQueuedPage();
    }

    public void showOnlyMyBuildsView() {
        buildEditorPage.setOnlyMyBuildsFilters();
        activateBuildPage();
    }

    public void showTodaysBuildsForDefinitionView(final IBuildDefinition buildDefinition) {
        buildEditorPage.setTodaysBuildsForDefinitionFilters(buildDefinition);
        activateBuildPage();
    }

    public void showControllerQueueView(final String controllerURI) {
        queueEditorPage.setControllerQueueViewFilters(controllerURI);
        activateQueuedPage();
    }

    /**
     * @see org.eclipse.ui.part.MultiPageEditorPart#createPages()
     */
    @Override
    protected void createPages() {
        getSite().getKeyBindingService().setScopes(new String[] {
            "com.microsoft.tfs.client.common.ui.teambuild.buildexplorer" //$NON-NLS-1$
        });

        if (buildServer == null) {
            return;
        }

        if (!buildServer.getBuildServerVersion().isV1()) {
            createQueuedBuildPage();
        }
        createCompletedBuildPage();
        updateTitle();

        serverManagerListener = new ServerManagerAdapter() {
            @Override
            public void onServerAdded(final ServerManagerEvent event) {
                UIHelpers.runOnUIThread(true, new Runnable() {
                    @Override
                    public void run() {
                        final TFSServer server = ((BuildExplorerEditorInput) getEditorInput()).getServer();

                        if (event.getServer() == server) {
                            setConnected(true);
                        } else if (event.getServer().connectionsEquivalent(server)) {
                            ((BuildExplorerEditorInput) getEditorInput()).setServer(event.getServer());
                            setConnected(true);
                        }
                    }
                });
            }

            @Override
            public void onServerRemoved(final ServerManagerEvent event) {
                UIHelpers.runOnUIThread(true, new Runnable() {
                    @Override
                    public void run() {
                        final TFSServer server = ((BuildExplorerEditorInput) getEditorInput()).getServer();
                        if (event.getServer() == server) {
                            setConnected(false);
                        } else if (event.getServer().connectionsEquivalent(server)) {
                            ((BuildExplorerEditorInput) getEditorInput()).setServer(event.getServer());
                            setConnected(false);
                        }
                    }
                });
            }
        };
        TFSCommonUIClientPlugin.getDefault().getProductPlugin().getServerManager().addListener(serverManagerListener);

        pollResults();
    }

    private void setConnected(final boolean connected) {
        isConnected = connected;

        if (!disposed) {
            if (buildEditorPage != null) {
                buildEditorPage.setEnabled(connected);
            }
            if (queueEditorPage != null) {
                queueEditorPage.setEnabled(connected);
            }
        }
    }

    private void createCompletedBuildPage() {
        buildEditorPage = new BuildEditorPage();
        try {
            buildPage = addPage(buildEditorPage, getEditorInput());
            setPageText(buildPage, Messages.getString("BuildExplorer.CompletedPageLabelText")); //$NON-NLS-1$
            setPageImage(buildPage, completedPageImage);
            getSite().registerContextMenu("Teambuild.BuildDetail", buildEditorPage.getContextMenu(), buildEditorPage); //$NON-NLS-1$
            buildEditorPage.getContextMenu().addMenuListener(new IMenuListener() {
                @Override
                public void menuAboutToShow(final IMenuManager manager) {
                    addRefreshMenuAction(manager);
                }
            });
            // Hook combo listeners
            buildEditorPage.getBuildDefinitionFilterCombo().addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(final SelectionEvent e) {
                    setName(buildEditorPage.getSelectedBuildDefinition());
                    if (queueEditorPage != null) {
                        queueEditorPage.setSelectedBuildDefinition(buildEditorPage.getSelectedBuildDefinition());
                    }
                }
            });
            setActivePage(buildPage);
        } catch (final PartInitException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void setName(final IBuildDefinition selectedBuildDefinition) {
        if (selectedBuildDefinition == null) {
            return;
        }

        final String messageFormat = Messages.getString("BuildExplorer.PartNameFormat"); //$NON-NLS-1$
        String title;

        if (selectedBuildDefinition.getName() != null
            && !BuildPath.RECURSION_OPERATOR.equals(selectedBuildDefinition.getName())) {
            title = MessageFormat.format(messageFormat, selectedBuildDefinition.getName());
        } else {
            title = MessageFormat.format(messageFormat, selectedBuildDefinition.getTeamProject());
        }

        setPartName(title);
    }

    protected void addRefreshMenuAction(final IMenuManager manager) {
        manager.add(refreshAction);
    }

    private void createQueuedBuildPage() {
        queueEditorPage = new QueueEditorPage();

        try {
            queuePage = addPage(queueEditorPage, getEditorInput());
            setPageText(queuePage, Messages.getString("BuildExplorer.QueuedPageLabelText")); //$NON-NLS-1$
            setPageImage(queuePage, queuedPageImage);
            getSite().registerContextMenu("Teambuild.QueuedBuild", queueEditorPage.getContextMenu(), queueEditorPage); //$NON-NLS-1$
            queueEditorPage.getContextMenu().addMenuListener(new IMenuListener() {
                @Override
                public void menuAboutToShow(final IMenuManager manager) {
                    addRefreshMenuAction(manager);
                }
            });
            queueEditorPage.getBuildDefinitionFilterCombo().addSelectionListener(new SelectionAdapter() {

                @Override
                public void widgetSelected(final SelectionEvent e) {
                    setName(queueEditorPage.getSelectedBuildDefinition());
                    buildEditorPage.setSelectedBuildDefinition(queueEditorPage.getSelectedBuildDefinition());
                }

            });
        } catch (final PartInitException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    private void updateTitle() {
        final IEditorInput input = getEditorInput();
        setTitleToolTip(input.getToolTipText());
        setPartName(input.getName());
    }

    /**
     * @see org.eclipse.ui.part.EditorPart#doSave(org.eclipse.core.runtime.IProgressMonitor)
     */
    @Override
    public void doSave(final IProgressMonitor monitor) {
        // DO NOTHING - should never be called.
    }

    /**
     * @see org.eclipse.ui.part.EditorPart#doSaveAs()
     */
    @Override
    public void doSaveAs() {
        // DO NOTHING - should never be called.
    }

    /**
     * @see org.eclipse.ui.part.EditorPart#isSaveAsAllowed()
     */
    @Override
    public boolean isSaveAsAllowed() {
        return false;
    }

    /**
     * @see org.eclipse.ui.part.MultiPageEditorPart#isDirty()
     */
    @Override
    public boolean isDirty() {
        return false;
    }

    /**
     * @see org.eclipse.ui.part.MultiPageEditorPart#dispose()
     */
    @Override
    public void dispose() {
        super.dispose();

        if (pollJob != null && (pollJob.getState() == Job.SLEEPING || pollJob.getState() == Job.WAITING)) {
            pollJob.cancel();
            pollJob = null;
        }

        if (serverManagerListener != null) {
            TFSCommonUIClientPlugin.getDefault().getProductPlugin().getServerManager().removeListener(
                serverManagerListener);
            serverManagerListener = null;
        }

        queuedPageImage.dispose();
        completedPageImage.dispose();
        if (queueEditorPage != null) {
            queueEditorPage.dispose();
        }
        buildEditorPage.dispose();
        instance = null;

        disposed = true;
    }

    /**
     * @return the disposed
     */
    public boolean isDisposed() {
        return disposed;
    }

    /**
     * @see org.eclipse.ui.part.MultiPageEditorPart#pageChange(int)
     */
    @Override
    protected void pageChange(final int newPage) {
        super.pageChange(newPage);
        if (newPage < 0) {
            return;
        }
        updatePageResults(newPage);
    }

    protected void updatePageResults(final int newPage) {
        if (newPage < 0 || !isConnected) {
            return;
        }

        if (newPage == queuePage) {
            queueEditorPage.updateResults();
        }
        if (newPage == buildPage) {
            buildEditorPage.updateResults();
        }
    }

    protected void pollResults() {
        // May possibly have come to this from an outstanding job.
        if (isDisposed()) {
            return;
        }

        /*
         * Perform refresh. If we're not isConnected, defer this refresh, but
         * queue the next one. (Otherwise, we wouldn't auto-refresh after
         * reconnecting.)
         */
        if (isConnected) {
            refresh();
        }

        // Reschedule a new pollJob
        pollJob = new Job(Messages.getString("BuildExplorer.RefreshingBuildStatus")) //$NON-NLS-1$
        {
            @Override
            protected IStatus run(final IProgressMonitor monitor) {
                pollResults();
                return Status.OK_STATUS;
            }
        };
        pollJob.setUser(false);
        pollJob.setSystem(true);
        pollJob.setPriority(Job.DECORATE);
        pollJob.schedule(pollInterval);
    }

    public void refresh() {
        updatePageResults(getActivePage());
    }

    /**
     * @return the pollInterval (in milliseconds)
     */
    public int getPollInterval() {
        return pollInterval;
    }

    /**
     * @param pollInterval
     *        the pollInterval to set (in milliseconds)
     */
    public void setPollInterval(final int pollInterval) {
        this.pollInterval = pollInterval;
    }

    @Override
    public boolean closeOnConnectionChange() {
        return true;
    }

    /**
     * Remove the passed builds from the completed tab and refresh the queued
     * tab.
     *
     * @param deletedBuilds
     *        Builds that have already been removed from the server.
     */
    public void removeBuilds(final IBuildDetail[] deletedBuilds) {
        queueEditorPage.updateResults();
        buildEditorPage.removeBuilds(deletedBuilds);
    }

    public void update() {
        buildEditorPage.getBuildsTableControl().getViewer().refresh();
    }

    public BuildEditorPage getBuildEditorPage() {
        return buildEditorPage;
    }

    /**
     * @return the queueEditorPage
     */
    public QueueEditorPage getQueueEditorPage() {
        return queueEditorPage;
    }

    public static BuildExplorer getInstance() {
        return instance;
    }

    public void reloadBuildDefinitions() {
        if (buildEditorPage != null) {
            buildEditorPage.reloadBuildDefinitions();
            buildDefinition = buildEditorPage.getSelectedBuildDefinition();
            setName(buildDefinition);
        }
        if (queueEditorPage != null) {
            queueEditorPage.reloadBuildDefinitions();
        }
    }

    public boolean isConnected() {
        return isConnected;
    }
}
