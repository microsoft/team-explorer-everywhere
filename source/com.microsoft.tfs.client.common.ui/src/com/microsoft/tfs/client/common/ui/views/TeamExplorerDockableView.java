// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.views;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.ViewPart;

import com.microsoft.tfs.client.common.autoconnect.AutoConnector;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.controls.teamexplorer.TeamExplorerDockableControl;
import com.microsoft.tfs.client.common.ui.framework.helper.UIHelpers;
import com.microsoft.tfs.client.common.ui.productplugin.TFSProductPlugin;
import com.microsoft.tfs.client.common.ui.teamexplorer.TeamExplorerContext;
import com.microsoft.tfs.client.common.ui.teamexplorer.internal.TeamExplorerConfig;

/**
 * The abstract dockable view which all dockable views should extend.
 */
public abstract class TeamExplorerDockableView extends ViewPart implements ITeamExplorerView {
    private final TeamExplorerConfig configuration;

    protected final TeamExplorerContext context;

    protected TeamExplorerDockableControl control;

    private final TFSProductPlugin plugin;

    private UndockedViewRepositoryListener listener;

    private final String pageID; // ID of the page to display in this view

    public TeamExplorerDockableView() {
        configuration = new TeamExplorerConfig();
        context = new TeamExplorerContext(this);

        plugin = TFSCommonUIClientPlugin.getDefault().getProductPlugin();
        pageID = getPageID();
    }

    @Override
    public void init(final IViewSite site) throws PartInitException {
        super.init(site);
        start();
    }

    private void start() {
        UIHelpers.runOnUIThread(true, new Runnable() {
            @Override
            public void run() {
                TFSCommonUIClientPlugin.getDefault().newViewShows(pageID);
            }
        });

        final AutoConnector connector = plugin.getAutoConnector();
        if (!connector.isStarted()) {
            connector.start();
        }

        listener = new UndockedViewRepositoryListener(this, context.getDefaultRepository());
        plugin.getRepositoryManager().addListener(listener);
        TFSCommonUIClientPlugin.getDefault().addProjectAndTeamListener(listener);
    }

    @Override
    public void dispose() {
        super.dispose();
        plugin.getRepositoryManager().removeListener(listener);
        TFSCommonUIClientPlugin.getDefault().removeProjectAndTeamListener(listener);

        getViewSite().getPage().hideView(this); // close view
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void createPartControl(final Composite parent) {
        initialize();
        control = new TeamExplorerDockableControl(pageID, configuration, context, parent, SWT.NONE);
    }

    /**
     * Extend this method to set specific page ID to reuse UI; The pageID is the
     * same as the ID of the page which you want to create the same UI control
     * in this view.
     *
     */
    protected abstract String getPageID();

    /**
     * Extend this method to do specific initialize work.
     *
     */
    protected void initialize() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setFocus() {
        control.setFocus();
    }

    @Override
    public TeamExplorerContext getContext() {
        return context;
    }

    @Override
    public void refresh() {
        control.refreshView();
    }
}
