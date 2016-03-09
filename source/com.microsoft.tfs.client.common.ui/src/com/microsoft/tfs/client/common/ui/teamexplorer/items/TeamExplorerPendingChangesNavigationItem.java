// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teamexplorer.items;

import com.microsoft.tfs.client.common.ui.teamexplorer.TeamExplorerContext;
import com.microsoft.tfs.core.clients.versioncontrol.SourceControlCapabilityFlags;

public class TeamExplorerPendingChangesNavigationItem extends TeamExplorerBaseNavigationItem {
    @Override
    public boolean isVisible(final TeamExplorerContext context) {
        if (!context.isConnected()) {
            return true;
        } else {
            final SourceControlCapabilityFlags flags = context.getSourceControlCapability();
            return flags.contains(SourceControlCapabilityFlags.TFS);
        }
    }
}
