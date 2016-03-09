// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.productplugin;

/**
 * Provides the running product plugin so lower layers (common client, core) can
 * make decisions about connections, preferences, etc.
 *
 * @threadsafety unknown
 */
public interface TFSProductPluginProvider {
    /**
     * @return the currently running {@link TFSProductPlugin}.
     */
    TFSProductPlugin getProductPlugin();
}
