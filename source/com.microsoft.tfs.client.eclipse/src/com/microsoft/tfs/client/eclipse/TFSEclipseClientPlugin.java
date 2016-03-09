// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.osgi.framework.BundleContext;

import com.microsoft.tfs.client.common.console.NullConsole;
import com.microsoft.tfs.client.common.console.TFSConsoleProvider;
import com.microsoft.tfs.client.common.console.TFSEclipseConsole;
import com.microsoft.tfs.client.common.logging.TELoggingConfiguration;
import com.microsoft.tfs.client.common.repository.RepositoryManager;
import com.microsoft.tfs.client.common.server.ServerManager;
import com.microsoft.tfs.client.common.util.ExtensionLoader;
import com.microsoft.tfs.client.eclipse.project.ProjectRepositoryManager;
import com.microsoft.tfs.client.eclipse.refresh.ResourceRefreshManager;
import com.microsoft.tfs.client.eclipse.resourcechange.TFSResourceChangeListener;
import com.microsoft.tfs.client.eclipse.resourcedata.ResourceDataManager;
import com.microsoft.tfs.core.product.ProductInformation;
import com.microsoft.tfs.core.product.ProductName;
import com.microsoft.tfs.core.telemetry.TfsTelemetryHelper;

/**
 * The activator class controls the plug-in life cycle
 *
 */
public final class TFSEclipseClientPlugin extends Plugin {
    public static final String PLUGIN_ID = "com.microsoft.tfs.client.eclipse"; //$NON-NLS-1$

    private static final Log log = LogFactory.getLog(TFSEclipseClientPlugin.class);

    public static final String TFS_CONSOLE_EXTENSION_POINT_ID = "com.microsoft.tfs.client.eclipse.consoleProvider"; //$NON-NLS-1$

    private static TFSEclipseClientPlugin plugin;

    private final ServerManager serverManager = new ServerManager();
    private final RepositoryManager repositoryManager = new RepositoryManager();
    private final ProjectRepositoryManager projectManager =
        new ProjectRepositoryManager(serverManager, repositoryManager);

    private final ResourceRefreshManager resourceRefreshManager = new ResourceRefreshManager(repositoryManager);

    /*
     * ResourceDataManager listens to core events, adapts those events into
     * information to be saved in the workspace's ISynchronizer store, and
     * queues those updates. It listens to workspace resource change events to
     * dequeue the updates (writes them to the ISynchronizer store). This must
     * be done in response to workspace change events because it needs to know
     * the state of the resources after they are fully modified by the core
     * operation.
     */
    private final ResourceDataManager resourceDataManager =
        new ResourceDataManager(ResourcesPlugin.getWorkspace().getSynchronizer());

    /*
     * The main resource change listener for the plugin exists to pick up added
     * files and pends changes for them. It ignores changes that are not adds
     * (moves, renames, etc. are handled elsewhere).
     */
    private final TFSResourceChangeListener resourceChangedListener = new TFSResourceChangeListener();

    private final Object consoleLock = new Object();
    private TFSConsoleProvider consoleProvider = null;

    public TFSEclipseClientPlugin() {
        ProductInformation.initialize(ProductName.PLUGIN);

        TELoggingConfiguration.configure();
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.eclipse.core.runtime.Plugin#start(org.osgi.framework.BundleContext)
     */
    @Override
    public void start(final BundleContext context) throws Exception {
        super.start(context);

        plugin = this;

        ResourcesPlugin.getWorkspace().addResourceChangeListener(
            resourceChangedListener,
            IResourceChangeEvent.POST_CHANGE);

        /*
         * Queue a job to notify the project repository manager to start. We
         * cannot simply do this work (we must put it in a job) to ensure that
         * the workbench is started. When the workbench is started, it will
         * start the JobManager which will execute our startup hook here.
         */
        final Job projectStartupJob = new Job(Messages.getString("TFSEclipseClientPlugin.ConnectintToTfsJobTitle")) //$NON-NLS-1$
        {
            @Override
            protected IStatus run(final IProgressMonitor progressMonitor) {
                projectManager.start();
                return Status.OK_STATUS;
            }
        };
        projectStartupJob.setSystem(true);
        projectStartupJob.schedule();

        TfsTelemetryHelper.sendSessionBegins();
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.eclipse.core.runtime.Plugin#stop(org.osgi.framework.BundleContext)
     */
    @Override
    public void stop(final BundleContext context) throws Exception {
        TfsTelemetryHelper.sendSessionEnds();

        ResourcesPlugin.getWorkspace().removeResourceChangeListener(resourceChangedListener);

        plugin = null;
        super.stop(context);
    }

    /**
     * Returns the shared instance
     *
     * @return the shared instance
     */
    public static TFSEclipseClientPlugin getDefault() {
        return plugin;
    }

    public ServerManager getServerManager() {
        return serverManager;
    }

    public RepositoryManager getRepositoryManager() {
        return repositoryManager;
    }

    public ProjectRepositoryManager getProjectManager() {
        return projectManager;
    }

    public ResourceDataManager getResourceDataManager() {
        return resourceDataManager;
    }

    public TFSResourceChangeListener getResourceChangeListener() {
        return resourceChangedListener;
    }

    public ResourceRefreshManager getResourceRefreshManager() {
        return resourceRefreshManager;
    }

    /**
     * @return the {@link TFSConsoleProvider} contributed via extension point,
     *         or <code>null</code> if none found
     */
    private final TFSConsoleProvider getConsoleProvider() {
        synchronized (consoleLock) {
            if (consoleProvider == null) {
                try {
                    consoleProvider =
                        (TFSConsoleProvider) ExtensionLoader.loadSingleExtensionClass(TFS_CONSOLE_EXTENSION_POINT_ID);
                } catch (final Exception e) {
                    log.error("Could not load TFS console provider for the product", e); //$NON-NLS-1$
                    consoleProvider = null;
                }
            }

            return consoleProvider;
        }
    }

    public TFSEclipseConsole getConsole() {
        final TFSConsoleProvider consoleProvider = getConsoleProvider();
        if (consoleProvider != null) {
            return consoleProvider.getConsole();
        } else {
            return new NullConsole();
        }

    }
}