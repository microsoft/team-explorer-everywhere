// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teambuild.teamexplorer.actions;

import org.eclipse.jface.action.IAction;

import com.microsoft.tfs.client.common.ui.teambuild.actions.BuildDetailAction;
import com.microsoft.tfs.client.common.ui.teambuild.teamexplorer.helpers.BuildHelpers;
import com.microsoft.tfs.core.clients.build.IBuildDetail;
import com.microsoft.tfs.util.Check;

public class ViewControllerQueueForBuildDetailAction extends BuildDetailAction {
    @Override
    public void run(final IAction action) {
        final IBuildDetail buildDetail = getSelectedBuildDetail();
        Check.notNull(buildDetail, "buildDetail"); //$NON-NLS-1$

        BuildHelpers.viewControllerQueue(buildDetail.getBuildServer(), buildDetail.getBuildDefinition());
    }
}
