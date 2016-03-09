// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teambuild.actions;

import org.eclipse.jface.action.IAction;

import com.microsoft.tfs.client.common.ui.teambuild.teamexplorer.helpers.BuildHelpers;
import com.microsoft.tfs.core.clients.build.IBuildDetail;

public class ViewBuildReportAction extends BuildDetailAction {
    /**
     * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
     */
    @Override
    public void run(final IAction action) {
        final IBuildDetail buildDetail = getSelectedBuildDetail();

        if (buildDetail != null) {
            BuildHelpers.viewBuildReport(getShell(), buildDetail);
        }
    }
}
