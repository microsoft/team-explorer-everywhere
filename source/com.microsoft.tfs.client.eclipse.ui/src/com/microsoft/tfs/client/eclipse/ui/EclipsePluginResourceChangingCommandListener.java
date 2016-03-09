// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.ui;

import com.microsoft.tfs.client.common.framework.resources.command.ResourceChangingCommandListener;
import com.microsoft.tfs.client.eclipse.TFSEclipseClientPlugin;

public class EclipsePluginResourceChangingCommandListener implements ResourceChangingCommandListener {
    public EclipsePluginResourceChangingCommandListener() {
    }

    @Override
    public void commandStarted() {
        /**
         * Plug-in could be null if it has been shut down.
         */
        final TFSEclipseClientPlugin plugin = TFSEclipseClientPlugin.getDefault();
        if (plugin != null) {
            plugin.getResourceChangeListener().startIgnoreThreadResourceChangeEvents();
        }
    }

    @Override
    public void commandFinished() {
        /**
         * Plug-in could be null if it has been shut down.
         */
        final TFSEclipseClientPlugin plugin = TFSEclipseClientPlugin.getDefault();
        if (plugin != null) {
            plugin.getResourceChangeListener().stopIgnoreThreadResourceChangeEvents();
        }
    }
}
