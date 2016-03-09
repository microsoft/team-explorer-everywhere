// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teambuild;

import org.eclipse.core.runtime.IAdapterManager;
import org.eclipse.core.runtime.Platform;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import com.microsoft.tfs.core.clients.build.IBuildDetail;
import com.microsoft.tfs.core.clients.build.IQueuedBuild;

/**
 * The activator class controls the plug-in life cycle
 */
public class TFSTeamBuildPlugin extends AbstractUIPlugin {
    // The plug-in ID
    public static final String PLUGIN_ID = "com.microsoft.tfs.client.common.ui.teambuild"; //$NON-NLS-1$

    // The shared instance
    private static TFSTeamBuildPlugin plugin;

    private TeamBuildAdapterFactory teamBuildAdapterFactory;

    /**
     * The constructor
     */
    public TFSTeamBuildPlugin() {
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
        teamBuildAdapterFactory = new TeamBuildAdapterFactory();
        final IAdapterManager manager = Platform.getAdapterManager();
        manager.registerAdapters(teamBuildAdapterFactory, IQueuedBuild.class);
        manager.registerAdapters(teamBuildAdapterFactory, IBuildDetail.class);
    }

    /**
     * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
     */
    @Override
    public void stop(final BundleContext context) throws Exception {
        plugin = null;
        super.stop(context);
        Platform.getAdapterManager().unregisterAdapters(teamBuildAdapterFactory);
        teamBuildAdapterFactory = null;
    }

    /**
     * Returns the shared instance
     *
     * @return the shared instance
     */
    public static TFSTeamBuildPlugin getDefault() {
        return plugin;
    }

}
