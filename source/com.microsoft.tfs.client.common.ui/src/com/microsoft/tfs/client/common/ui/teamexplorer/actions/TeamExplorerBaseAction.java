// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teamexplorer.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.ui.IWorkbenchPart;

import com.microsoft.tfs.client.common.ui.framework.action.ObjectActionDelegate;
import com.microsoft.tfs.client.common.ui.framework.telemetry.ClientTelemetryHelper;
import com.microsoft.tfs.client.common.ui.teamexplorer.TeamExplorerContext;
import com.microsoft.tfs.client.common.ui.views.ITeamExplorerView;
import com.microsoft.tfs.util.Check;

public abstract class TeamExplorerBaseAction extends ObjectActionDelegate {
    public TeamExplorerContext getContext() {
        final IWorkbenchPart part = getTargetPart();
        Check.isTrue(part instanceof ITeamExplorerView);
        return ((ITeamExplorerView) part).getContext();
    }

    @Override
    public final void run(final IAction action) {
        ClientTelemetryHelper.sendRunActionEvent(this);
        doRun(action);
    }

    public abstract void doRun(IAction action);
}
