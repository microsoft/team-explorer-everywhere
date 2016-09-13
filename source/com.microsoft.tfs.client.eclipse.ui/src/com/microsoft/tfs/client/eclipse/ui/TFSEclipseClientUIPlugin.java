// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.ui;

import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import com.microsoft.tfs.client.common.autoconnect.AutoConnector;
import com.microsoft.tfs.client.common.connectionconflict.ConnectionConflictHandler;
import com.microsoft.tfs.client.common.framework.command.CommandFactory;
import com.microsoft.tfs.client.common.framework.command.ICommand;
import com.microsoft.tfs.client.common.repository.RepositoryManager;
import com.microsoft.tfs.client.common.repository.RepositoryManagerAdapter;
import com.microsoft.tfs.client.common.repository.RepositoryManagerEvent;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.server.ServerManager;
import com.microsoft.tfs.client.common.ui.framework.command.UIJobCommandAdapter;
import com.microsoft.tfs.client.common.ui.helpers.FileViewer;
import com.microsoft.tfs.client.common.ui.productplugin.TFSProductPlugin;
import com.microsoft.tfs.client.common.ui.protocolhandler.ProtocolHandler;
import com.microsoft.tfs.client.common.ui.wizard.teamprojectwizard.ITeamProjectWizard;
import com.microsoft.tfs.client.eclipse.TFSEclipseClientPlugin;
import com.microsoft.tfs.client.eclipse.ui.connectionconflict.EclipseUIConnectionConflictHandler;
import com.microsoft.tfs.client.eclipse.ui.projectcreation.TFSProjectCreationListener;
import com.microsoft.tfs.client.eclipse.ui.resourcechange.IgnoreFileResourceChangeListener;
import com.microsoft.tfs.client.eclipse.ui.viewer.EclipseFileViewer;
import com.microsoft.tfs.client.eclipse.ui.wizard.teamprojectwizard.EclipseTeamProjectWizard;
import com.microsoft.tfs.core.clients.versioncontrol.WorkspaceLocation;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.core.product.ProductInformation;
import com.microsoft.tfs.core.product.ProductName;

public class TFSEclipseClientUIPlugin extends AbstractUIPlugin implements TFSProductPlugin {
    public static final String PLUGIN_ID = "com.microsoft.tfs.client.eclipse.ui"; //$NON-NLS-1$

    private static TFSEclipseClientUIPlugin plugin;

    /*
     * A resource change listener to refresh label decorations when .tpignore or
     * .tfignore files change.
     */
    private final IgnoreFileResourceChangeListener ignoreFileResourceChangeListener =
        new IgnoreFileResourceChangeListener();

    /*
     * A secondary resource change listener for the plugin exists to locate new
     * project creations that occur for mapped paths and automatically configure
     * TFS as the repository provider.
     */
    private final TFSProjectCreationListener projectCreationListener = new TFSProjectCreationListener();

    private final AutoConnector autoConnector = new EclipseAutoConnector();
    private final ConnectionConflictHandler connectionConflictHandler = new EclipseUIConnectionConflictHandler();

    private final RepositoryListener repositoryListener = new RepositoryListener();

    public TFSEclipseClientUIPlugin() {
        ProductInformation.initialize(ProductName.PLUGIN);

        plugin = this;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.
     * BundleContext )
     */
    @Override
    public void start(final BundleContext context) throws Exception {
        super.start(context);

        ResourcesPlugin.getWorkspace().addResourceChangeListener(
            ignoreFileResourceChangeListener,
            IResourceChangeEvent.POST_CHANGE);

        ResourcesPlugin.getWorkspace().addResourceChangeListener(
            projectCreationListener,
            IResourceChangeEvent.POST_CHANGE);

        getRepositoryManager().addListener(repositoryListener);

        registerProtocolHandler();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.
     * BundleContext )
     */
    @Override
    public void stop(final BundleContext context) throws Exception {
        ResourcesPlugin.getWorkspace().removeResourceChangeListener(ignoreFileResourceChangeListener);
        ResourcesPlugin.getWorkspace().removeResourceChangeListener(projectCreationListener);

        getRepositoryManager().removeListener(repositoryListener);

        plugin = null;

        super.stop(context);
    }

    /**
     * Returns the shared instance
     *
     * @return the shared instance
     */
    public static TFSEclipseClientUIPlugin getDefault() {
        return plugin;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AutoConnector getAutoConnector() {
        return autoConnector;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConnectionConflictHandler getConnectionConflictHandler() {
        return connectionConflictHandler;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ServerManager getServerManager() {
        return TFSEclipseClientPlugin.getDefault().getServerManager();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RepositoryManager getRepositoryManager() {
        return TFSEclipseClientPlugin.getDefault().getRepositoryManager();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TFSRepository[] getImmutableRepositories() {
        /* In-use workspaces can't be deleted. */
        return TFSEclipseClientPlugin.getDefault().getProjectManager().getRepositories();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getMementoPreferenceKeyPrefix() {
        return "com.microsoft.tfs.client.eclipse"; //$NON-NLS-1$
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ITeamProjectWizard getTeamProjectWizard() {
        return new EclipseTeamProjectWizard();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FileViewer getFileViewer() {
        return new EclipseFileViewer();
    }

    private void registerProtocolHandler() {
        final ICommand registrationCommand = ProtocolHandler.getInstance().getRegistrationCommand();

        if (registrationCommand != null) {
            final UIJobCommandAdapter registrationJob =
                new UIJobCommandAdapter(CommandFactory.newCancelableCommand(registrationCommand), null, null);

            registrationJob.schedule();
        }
    }

    private class RepositoryListener extends RepositoryManagerAdapter {
        @Override
        public void onDefaultRepositoryChanged(final RepositoryManagerEvent event) {
            final TFSRepository repository = event.getRepository();

            if (repository != null) {
                final Workspace workspace = repository.getWorkspace();

                if (workspace != null && WorkspaceLocation.LOCAL == workspace.getLocation()) {
                    final Job reconcileJob =
                        new Job(Messages.getString("TFSEclipseClientUIPlugin.ReconcilingLocalWorkspace")) //$NON-NLS-1$
                        {
                            @Override
                            public IStatus run(final IProgressMonitor monitor) {
                                final AtomicBoolean pendingChangesUpdatedByServer = new AtomicBoolean();

                                workspace.reconcile(false, pendingChangesUpdatedByServer);

                                if (pendingChangesUpdatedByServer.get()) {
                                    repository.getPendingChangeCache().refresh();
                                }
                                return Status.OK_STATUS;
                            }
                        };
                    reconcileJob.schedule();
                }
            }
        }
    }
}
