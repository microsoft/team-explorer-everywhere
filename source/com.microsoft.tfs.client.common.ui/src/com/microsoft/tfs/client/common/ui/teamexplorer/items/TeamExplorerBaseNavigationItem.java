// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teamexplorer.items;

import com.microsoft.tfs.client.common.ui.framework.telemetry.ClientTelemetryHelper;
import com.microsoft.tfs.client.common.ui.teamexplorer.TeamExplorerContext;

/**
 * The base class of all Team Explorer navigation items with default behaviors.
 */
public class TeamExplorerBaseNavigationItem implements ITeamExplorerNavigationItem {
    @Override
    public boolean isVisible(final TeamExplorerContext context) {
        return context.isConnected();
    }

    @Override
    public final void clicked(final TeamExplorerContext context) {
        ClientTelemetryHelper.sendTeamExplorerPageView(getClass().getName(), null);

        onClick(context);
    }

    protected void onClick(final TeamExplorerContext context) {

    }

    @Override
    public boolean canOpenInWeb() {
        return false;
    }

    @Override
    public void openInWeb(final TeamExplorerContext context) {
    }
}
