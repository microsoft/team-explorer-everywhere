// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.checkinpolicies;

import org.eclipse.core.runtime.Plugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class TFSCheckinPoliciesPlugin extends Plugin {
    // The plug-in ID
    public static final String PLUGIN_ID = "com.microsoft.tfs.checkinpolicies"; //$NON-NLS-1$

    // The shared instance
    private static TFSCheckinPoliciesPlugin plugin;

    /**
     * The constructor
     */
    public TFSCheckinPoliciesPlugin() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void start(final BundleContext context) throws Exception {
        super.start(context);
        plugin = this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop(final BundleContext context) throws Exception {
        plugin = null;
        super.stop(context);
    }

    /**
     * Returns the shared instance
     *
     * @return the shared instance
     */
    public static TFSCheckinPoliciesPlugin getDefault() {
        return plugin;
    }
}
