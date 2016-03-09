// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.ui.dialogs.vc;

import org.eclipse.core.resources.IProject;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.ui.framework.dialog.BaseDialog;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.framework.sizing.ControlSize;
import com.microsoft.tfs.client.eclipse.ui.Messages;
import com.microsoft.tfs.client.eclipse.ui.controls.ProjectTable;

public class ReturnOnlineProjectsDialog extends BaseDialog {
    private IProject[] projects;

    private ProjectTable projectTable;

    public ReturnOnlineProjectsDialog(final Shell shell) {
        super(shell);
    }

    @Override
    protected String provideDialogTitle() {
        return Messages.getString("ReturnOnlineProjectsDialog.DialogTitle"); //$NON-NLS-1$
    }

    @Override
    protected void hookAddToDialogArea(final Composite dialogArea) {
        final GridLayout dialogLayout = new GridLayout(1, false);
        dialogLayout.marginWidth = getHorizontalMargin();
        dialogLayout.marginHeight = getVerticalMargin();
        dialogLayout.horizontalSpacing = getHorizontalSpacing();
        dialogLayout.verticalSpacing = getVerticalSpacing();
        dialogArea.setLayout(dialogLayout);

        final Label descriptionLabel = new Label(dialogArea, SWT.WRAP);
        descriptionLabel.setText(Messages.getString("ReturnOnlineProjectsDialog.DescriptionLabelText")); //$NON-NLS-1$
        GridDataBuilder.newInstance().hGrab().hFill().applyTo(descriptionLabel);

        projectTable = new ProjectTable(dialogArea, SWT.NONE);

        if (projects != null) {
            projectTable.setProjects(projects);
        }
        GridDataBuilder.newInstance().grab().fill().applyTo(projectTable);
        ControlSize.setCharHeightHint(projectTable, 8);
    }

    public void setProjects(final IProject[] projects) {
        this.projects = projects;

        if (projectTable != null && !projectTable.isDisposed()) {
            projectTable.setProjects(projects);
        }
    }
}
