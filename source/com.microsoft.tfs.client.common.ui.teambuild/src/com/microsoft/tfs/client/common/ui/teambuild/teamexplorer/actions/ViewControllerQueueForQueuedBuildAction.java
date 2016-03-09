// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teambuild.teamexplorer.actions;

import org.eclipse.jface.action.IAction;

import com.microsoft.tfs.client.common.ui.teambuild.actions.QueuedBuildAction;
import com.microsoft.tfs.client.common.ui.teambuild.teamexplorer.helpers.BuildHelpers;
import com.microsoft.tfs.core.clients.build.IQueuedBuild;
import com.microsoft.tfs.util.Check;

public class ViewControllerQueueForQueuedBuildAction extends QueuedBuildAction {
    @Override
    public void doRun(final IAction action) {
        final IQueuedBuild queuedBuild = getSelectedQueuedBuild();
        Check.notNull(queuedBuild, "queuedBuild"); //$NON-NLS-1$

        BuildHelpers.viewControllerQueue(queuedBuild.getBuildServer(), queuedBuild.getBuildDefinition());
    }
}
