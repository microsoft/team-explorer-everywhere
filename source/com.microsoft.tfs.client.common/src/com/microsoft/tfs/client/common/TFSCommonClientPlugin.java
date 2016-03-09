// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.Plugin;

public class TFSCommonClientPlugin extends Plugin {
    private static final Log log = LogFactory.getLog(TFSCommonClientPlugin.class);

    public static final String PLUGIN_ID = "com.microsoft.tfs.client.common"; //$NON-NLS-1$

    private static TFSCommonClientPlugin plugin;

    public TFSCommonClientPlugin() {
        plugin = this;
    }

    public static TFSCommonClientPlugin getDefault() {
        return plugin;
    }
}
