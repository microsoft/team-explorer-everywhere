// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teambuild.actions;

import java.util.ArrayList;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;

import com.microsoft.tfs.client.common.ui.framework.telemetry.ClientTelemetryHelper;
import com.microsoft.tfs.client.common.ui.teambuild.editors.BuildExplorer;
import com.microsoft.tfs.core.clients.build.IBuildDetail;
import com.microsoft.tfs.core.clients.build.IQueuedBuild;
import com.microsoft.tfs.core.clients.build.flags.QueueStatus;

public abstract class QueuedBuildAction extends BaseAction {

    protected IBuildDetail getSelectedBuildDetail() {
        return getSelectedQueuedBuild().getBuild();
    }

    protected IBuildDetail[] getSelectedBuildDetails() {
        final ArrayList<IBuildDetail> list = new ArrayList<IBuildDetail>();
        final IQueuedBuild[] queuedBuilds = getSelectedQueuedBuilds();
        for (int i = 0; i < queuedBuilds.length; i++) {
            if (queuedBuilds[i].getBuild() != null) {
                list.add(queuedBuilds[i].getBuild());
            }
        }

        return list.toArray(new IBuildDetail[list.size()]);
    }

    protected IQueuedBuild getSelectedQueuedBuild() {
        return (IQueuedBuild) adaptSelectionFirstElement(IQueuedBuild.class);
    }

    protected IQueuedBuild[] getSelectedQueuedBuilds() {
        return (IQueuedBuild[]) adaptSelectionToArray(IQueuedBuild.class);
    }

    /**
     * @see com.microsoft.tfs.client.common.ui.teambuild.actions.BaseAction#onSelectionChanged(org.eclipse.jface.action.IAction,
     *      org.eclipse.jface.viewers.ISelection)
     */
    @Override
    protected void onSelectionChanged(final IAction action, final ISelection selection) {
        super.onSelectionChanged(action, selection);
        if (action.isEnabled()) {
            final Object obj = getSelectionFirstElement();
            if (!(obj instanceof IQueuedBuild)) {
                action.setEnabled(false);
                return;
            }

            if (BuildExplorer.getInstance() != null && !BuildExplorer.getInstance().isConnected()) {
                action.setEnabled(false);
            }

            final IQueuedBuild queuedBuild = ((IQueuedBuild) obj);

            if (queuedBuild == null || queuedBuild.getStatus() == null) {
                action.setEnabled(false);
                return;
            }
            action.setEnabled(
                !(queuedBuild.getStatus().contains(QueueStatus.COMPLETED) && queuedBuild.getBuild() != null));
        }
    }

    /**
     * NOT YET IMPLEMENTED
     *
     * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
     */
    public abstract void doRun(final IAction action);

    @Override
    public void run(final IAction action) {
        ClientTelemetryHelper.sendRunActionEvent(this);
        doRun(action);
    }

}
