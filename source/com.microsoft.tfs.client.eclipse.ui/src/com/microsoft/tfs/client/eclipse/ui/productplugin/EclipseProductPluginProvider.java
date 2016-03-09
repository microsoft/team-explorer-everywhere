// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.ui.productplugin;

import com.microsoft.tfs.client.common.ui.productplugin.TFSProductPlugin;
import com.microsoft.tfs.client.common.ui.productplugin.TFSProductPluginProvider;
import com.microsoft.tfs.client.eclipse.ui.TFSEclipseClientUIPlugin;

/**
 * Contributes the Eclipse UI product plugin to the common UI.
 *
 * @threadsafety unknown
 */
public class EclipseProductPluginProvider implements TFSProductPluginProvider {
    @Override
    public TFSProductPlugin getProductPlugin() {
        return TFSEclipseClientUIPlugin.getDefault();
    }
}
