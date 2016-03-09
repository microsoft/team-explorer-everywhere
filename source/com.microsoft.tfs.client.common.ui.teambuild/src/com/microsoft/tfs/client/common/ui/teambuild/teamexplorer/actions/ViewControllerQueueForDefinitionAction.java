// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teambuild.teamexplorer.actions;

import org.eclipse.jface.action.IAction;

import com.microsoft.tfs.client.common.ui.teambuild.teamexplorer.helpers.BuildHelpers;

public class ViewControllerQueueForDefinitionAction extends TeamExplorerSingleBuildDefinitionAction {
    @Override
    public void doRun(final IAction action) {
        BuildHelpers.viewControllerQueue(selectedDefinition.getBuildServer(), selectedDefinition);
    }
}
