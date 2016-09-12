// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

import com.microsoft.tfs.client.common.console.TFSConsoleProvider;
import com.microsoft.tfs.client.common.logging.TELoggingConfiguration;
import com.microsoft.tfs.client.common.ui.buildmanager.BuildManager;
import com.microsoft.tfs.client.common.ui.console.ConsoleCoreEventListener;
import com.microsoft.tfs.client.common.ui.console.TFSConsole;
import com.microsoft.tfs.client.common.ui.editors.SourceControlListener;
import com.microsoft.tfs.client.common.ui.framework.helper.ShellUtils;
import com.microsoft.tfs.client.common.ui.helpers.WorkbenchHelper;
import com.microsoft.tfs.client.common.ui.productplugin.TFSProductPlugin;
import com.microsoft.tfs.client.common.ui.productplugin.TFSProductPluginProvider;
import com.microsoft.tfs.client.common.ui.teamexplorer.NewViewShowsListener;
import com.microsoft.tfs.client.common.ui.teamexplorer.ProjectAndTeamListener;
import com.microsoft.tfs.client.common.ui.teamexplorer.internal.pendingchanges.PendingChangesViewModel;
import com.microsoft.tfs.client.common.util.ExtensionLoader;
import com.microsoft.tfs.core.clients.versioncontrol.Workstation;
import com.microsoft.tfs.core.config.persistence.DefaultPersistenceStoreProvider;
import com.microsoft.tfs.core.product.ProductInformation;
import com.microsoft.tfs.core.product.ProductName;
import com.microsoft.tfs.core.util.notifications.MessageWindowNotificationManager;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.Platform;
import com.microsoft.tfs.util.listeners.SingleListenerFacade;

/**
 * The activator class controls the plug-in life cycle.
 */
public class TFSCommonUIClientPlugin extends AbstractUIPlugin implements TFSConsoleProvider {
    // The plug-in ID
    public static final String PLUGIN_ID = "com.microsoft.tfs.client.common.ui"; //$NON-NLS-1$

    /**
     * Extension point which product plug-ins (Explorer, Team Explorer
     * Everywhere) hook into so this class can detect configuration information.
     */
    public static final String PRODUCT_PLUGIN_PROVIDER_EXTENSION_POINT_ID =
        "com.microsoft.tfs.client.common.ui.productPluginProvider"; //$NON-NLS-1$

    private static final Log log = LogFactory.getLog(TFSCommonUIClientPlugin.class);

    // The shared instance
    private static TFSCommonUIClientPlugin plugin;

    private ServiceTracker proxyServiceTracker;

    /* The TFS Console */
    private TFSConsole console;
    private ConsoleCoreEventListener consoleCoreEventListener;
    private final Object consoleLock = new Object();

    /* The product plugin provider for this application */
    private TFSProductPluginProvider productPluginProvider;
    private final Object productPluginProviderLock = new Object();

    /* The Build manager */
    private BuildManager buildManager;
    private final Object buildManagerLock = new Object();

    /* The pending changes view model */
    private PendingChangesViewModel pendingChangesViewModel;
    private final Object pendingChangesViewModelLock = new Object();

    /**
     * The constructor
     */
    public TFSCommonUIClientPlugin() {
        ProductInformation.initialize(ProductName.PLUGIN);

        TELoggingConfiguration.configure();
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
        plugin = this;

        /*
         * This Class.forName call is necessary because the org.eclipse.core.net
         * bundle has the Eclipse-LazyStart header set to true. This means that
         * the bundle does not start (and register its IProxyService) until some
         * other bundle attempts to load a class it provides. Normally this is
         * not an issue because the code that creates the service tracker would
         * pass IProxyService.class when creating the ServiceTracker, which
         * would lazily start the org.eclipse.core.net bundle, which would
         * register the IProxyService. However, we don't reference
         * IProxyService.class directly when we create the ServiceTracker
         * because of backwards compatibility reasons. Therefore, we must
         * attempt a class load here to lazily start the org.eclipse.core.net
         * bundle.
         */
        try {
            Class.forName("org.eclipse.core.net.proxy.IProxyService"); //$NON-NLS-1$
        } catch (final ClassNotFoundException e) {
        }

        proxyServiceTracker = new ServiceTracker(context, "org.eclipse.core.net.proxy.IProxyService", null); //$NON-NLS-1$
        proxyServiceTracker.open();

        /*
         * Create the console once the user-interface is fully formed (ie, on a
         * background thread that will run once the Workbench is started.) This
         * will ensure that we don't try to start it too early.
         */
        WorkbenchHelper.runOnWorkbenchStartup(
            Messages.getString("TFSCommonUiClientPlugin.CreatingTfsConsoleJobTitle"), //$NON-NLS-1$
            new Runnable() {
                @Override
                public void run() {
                    Display.getDefault().asyncExec(new Runnable() {
                        @Override
                        public void run() {
                            createConsole();
                        }
                    });
                }
            });

        /*
         * Run as an interactive job to guarantee the windowing system is ready
         * to use.
         */
        WorkbenchHelper.runOnWorkbenchStartup(
            Messages.getString("TFSCommonUIClientPlugin.InitializingNotificationManagerJobTitle"), //$NON-NLS-1$
            new Runnable() {
                @Override
                public void run() {
                    Display.getDefault().asyncExec(new Runnable() {
                        @Override
                        public void run() {
                            /*
                             * Messages received by the manager we construct
                             * will be routed to the Workstation for dispatch
                             * through any VersionControlClients' EventEngines
                             * in use.
                             */

                            if (Platform.isCurrentPlatform(Platform.WINDOWS)) {
                                Workstation.getCurrent(DefaultPersistenceStoreProvider.INSTANCE).setNotificationManager(
                                    new MessageWindowNotificationManager());
                            } else {
                                // TODO implement other mechanisms for other
                                // platforms
                            }
                        }
                    });
                }
            });
    }

    private void createConsole() {
        synchronized (consoleLock) {
            console = new TFSConsole();

            consoleCoreEventListener = new ConsoleCoreEventListener(console);
            consoleCoreEventListener.attach(getProductPlugin().getServerManager());
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.
     * BundleContext )
     */
    @Override
    public void stop(final BundleContext context) throws Exception {
        proxyServiceTracker.close();

        /*
         * Call detachAll() instead of detach(getServerManager()).
         * getServerManager() may return a null server manager when the plugin
         * is stopping, because it may need to query a server manager provider
         * in a plugin (for example, the Explorer application) which has already
         * stopped.
         */
        if (consoleCoreEventListener != null) {
            consoleCoreEventListener.detachAll();
        }

        Workstation.getCurrent(DefaultPersistenceStoreProvider.INSTANCE).setNotificationManager(null);

        plugin = null;
        super.stop(context);
    }

    public ServiceTracker getProxyServiceTracker() {
        return proxyServiceTracker;
    }

    /**
     * Gets the currently running {@link TFSProductPlugin} (Team Explorer
     * Everywhere or Explorer).
     *
     * @return the currently running {@link TFSProductPlugin} (never
     *         <code>null</code>)
     */
    public TFSProductPlugin getProductPlugin() {
        /*
         * If we do not yet know the product plugin, query the extensions
         * available.
         */
        synchronized (productPluginProviderLock) {
            if (productPluginProvider == null) {
                productPluginProvider = (TFSProductPluginProvider) ExtensionLoader.loadSingleExtensionClass(
                    PRODUCT_PLUGIN_PROVIDER_EXTENSION_POINT_ID);
            }

            final TFSProductPlugin productPlugin = productPluginProvider.getProductPlugin();

            Check.notNull(productPlugin, "productPlugin"); //$NON-NLS-1$

            return productPlugin;
        }
    }

    @Override
    public TFSConsole getConsole() {
        synchronized (consoleLock) {
            if (console == null) {
                createConsole();
            }

            return console;
        }
    }

    public BuildManager getBuildManager() {
        synchronized (buildManagerLock) {
            if (buildManager == null) {
                buildManager = new BuildManager();
            }

            return buildManager;
        }
    }

    public PendingChangesViewModel getPendingChangesViewModel() {
        synchronized (pendingChangesViewModelLock) {
            if (pendingChangesViewModel == null) {
                pendingChangesViewModel = new PendingChangesViewModel(ShellUtils.getWorkbenchShell());
            }

            return pendingChangesViewModel;
        }
    }

    /**
     * Returns the shared instance
     *
     * @return the shared instance
     */
    public static TFSCommonUIClientPlugin getDefault() {
        return plugin;
    }

    private final SingleListenerFacade sourceControlProviderChangedListeners =
        new SingleListenerFacade(SourceControlListener.class);

    public void addSourceControlListener(final SourceControlListener listener) {
        sourceControlProviderChangedListeners.addListener(listener);
    }

    public void removeSourceControlListener(final SourceControlListener listener) {
        sourceControlProviderChangedListeners.removeListener(listener);
    }

    private SourceControlListener getSourceControlListener() {
        return (SourceControlListener) sourceControlProviderChangedListeners.getListener();
    }

    public void sourceControlChanged(final boolean tfvc) {
        getSourceControlListener().onSourceControlChanged(tfvc);
    }

    private final SingleListenerFacade projectAndTeamListeners = new SingleListenerFacade(ProjectAndTeamListener.class);

    public void addProjectAndTeamListener(final ProjectAndTeamListener listener) {
        projectAndTeamListeners.addListener(listener);
    }

    public void removeProjectAndTeamListener(final ProjectAndTeamListener listener) {
        projectAndTeamListeners.removeListener(listener);
    }

    private ProjectAndTeamListener getProjectAndTeamListener() {
        return (ProjectAndTeamListener) projectAndTeamListeners.getListener();
    }

    public void projectOrTeamChanged() {
        getProjectAndTeamListener().onProjectOrTeamChanged();
    }

    private final SingleListenerFacade viewShowsListeners = new SingleListenerFacade(NewViewShowsListener.class);

    public void addViewShowsListener(final NewViewShowsListener listener) {
        viewShowsListeners.addListener(listener);
    }

    public void removeViewShowsListener(final NewViewShowsListener listener) {
        viewShowsListeners.removeListener(listener);
    }

    private NewViewShowsListener getViewShowListener() {
        return (NewViewShowsListener) viewShowsListeners.getListener();
    }

    public void newViewShows(final String pageID) {
        getViewShowListener().onNewViewShows(pageID);
    }
}
