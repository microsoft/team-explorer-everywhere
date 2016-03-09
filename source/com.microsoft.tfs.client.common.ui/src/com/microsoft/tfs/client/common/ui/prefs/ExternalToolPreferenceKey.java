// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.prefs;

import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;

/**
 * Accessor for the product-specific memento preference keys for external tools.
 *
 * @threadsafety unknown
 */
public final class ExternalToolPreferenceKey {
    public static final String VIEW_KEY =
        TFSCommonUIClientPlugin.getDefault().getProductPlugin().getMementoPreferenceKeyPrefix() + ".viewToolset"; //$NON-NLS-1$

    public static final String COMPARE_KEY =
        TFSCommonUIClientPlugin.getDefault().getProductPlugin().getMementoPreferenceKeyPrefix() + ".compareToolset"; //$NON-NLS-1$

    public static final String MERGE_KEY =
        TFSCommonUIClientPlugin.getDefault().getProductPlugin().getMementoPreferenceKeyPrefix() + ".mergeToolset"; //$NON-NLS-1$
}
