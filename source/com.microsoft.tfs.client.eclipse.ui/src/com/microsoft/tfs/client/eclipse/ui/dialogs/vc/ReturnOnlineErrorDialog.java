// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.ui.dialogs.vc;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.dialogs.IDialogConstants;
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

/**
 * Notifies that returning online failed for some projects.
 *
 * @threadsafety unknown
 */
public class ReturnOnlineErrorDialog extends BaseDialog {
    private IProject[] projects;

    private ProjectTable projectTable;

    public ReturnOnlineErrorDialog(final Shell shell) {
        super(shell);

        /* This is informative only, only provide an ok button. */
        setOptionIncludeDefaultButtons(false);
        addButtonDescription(IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
    }

    @Override
    protected String provideDialogTitle() {
        return Messages.getString("ReturnOnlineFailedDialog.DialogTitle"); //$NON-NLS-1$
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
        descriptionLabel.setText(Messages.getString("ReturnOnlineFailedDialog.DescriptionLabelText")); //$NON-NLS-1$
        GridDataBuilder.newInstance().hGrab().hFill().wHint(getMinimumMessageAreaWidth()).applyTo(descriptionLabel);

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
