// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teambuild.actions;

import java.util.ArrayList;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;

import com.microsoft.tfs.client.common.ui.teambuild.editors.BuildExplorer;
import com.microsoft.tfs.core.clients.build.IBuildDetail;
import com.microsoft.tfs.core.clients.build.IQueuedBuild;

public abstract class BuildDetailAction extends BaseAction {

    protected IBuildDetail getSelectedBuildDetail() {
        return (IBuildDetail) adaptSelectionFirstElement(IBuildDetail.class);
    }

    protected IBuildDetail[] getSelectedBuildDetails() {
        return (IBuildDetail[]) adaptSelectionToArray(IBuildDetail.class);
    }

    protected IBuildDetail[] getSeletedCompletedBuilds() {
        final IBuildDetail[] selectedBuilds = getSelectedBuildDetails();
        final ArrayList<IBuildDetail> completedBuilds = new ArrayList<IBuildDetail>(selectedBuilds.length);
        for (int i = 0; i < selectedBuilds.length; i++) {
            if (selectedBuilds[i].isBuildFinished()) {
                completedBuilds.add(selectedBuilds[i]);
            }
        }
        return completedBuilds.toArray(new IBuildDetail[completedBuilds.size()]);
    }

    @Override
    protected void onSelectionChanged(final IAction action, final ISelection selection) {
        super.onSelectionChanged(action, selection);
        if (action.isEnabled()) {
            if (BuildExplorer.getInstance() != null && !BuildExplorer.getInstance().isConnected()) {
                action.setEnabled(false);
            }

            final Object obj = getSelectionFirstElement();

            if (obj instanceof IQueuedBuild) {
                final IQueuedBuild qb = ((IQueuedBuild) obj);
                if (qb == null || qb.getBuild() == null || qb.getBuild().getURI() == null) {
                    action.setEnabled(false);
                }

            }
        }
    }

}
