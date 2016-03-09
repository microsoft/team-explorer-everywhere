// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.ui.wizard.importwizard;

import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.NewProjectAction;

import com.microsoft.tfs.client.eclipse.ui.wizard.importwizard.support.ImportNewProjectAction;

public class ImportWizardNewProjectAction implements ImportNewProjectAction {
    @Override
    public void run() {
        Display.getDefault().syncExec(new Runnable() {
            @Override
            public void run() {
                final NewProjectAction newProjectAction =
                    new NewProjectAction(PlatformUI.getWorkbench().getActiveWorkbenchWindow());
                newProjectAction.run();
            }
        });
    }
}
