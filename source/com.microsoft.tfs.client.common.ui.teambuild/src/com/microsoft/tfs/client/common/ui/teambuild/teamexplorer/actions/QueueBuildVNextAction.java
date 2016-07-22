// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teambuild.teamexplorer.actions;

import org.eclipse.jface.action.IAction;

import com.microsoft.alm.teamfoundation.build.webapi.DefinitionReference;
import com.microsoft.tfs.client.common.ui.tasks.QueueBuildVNextTask;

public class QueueBuildVNextAction extends BuildDefinitionVNextAction {
    /**
     * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
     */
    @Override
    public void doRun(final IAction action) {
        final DefinitionReference buildDefinition = getSelectedBuildDefinition();

        if (buildDefinition != null) {
            new QueueBuildVNextTask(getShell(), getConnection(), buildDefinition).run();
            ;
        }
    }
}
