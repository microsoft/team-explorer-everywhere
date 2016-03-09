// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.dialogs.workspaces;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.controls.workspaces.WorkspacesControl;
import com.microsoft.tfs.client.common.ui.framework.dialog.BaseDialog;
import com.microsoft.tfs.client.common.ui.framework.helper.SWTUtil;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.framework.validation.ButtonValidatorBinding;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.util.Check;

public class WorkspacesDialog extends BaseDialog {
    private final TFSTeamProjectCollection connection;
    private final boolean createWorkspaceIfNone;
    private final boolean workspaceRequired;
    private final String title;

    private WorkspacesControl control;

    public WorkspacesDialog(
        final Shell parentShell,
        final TFSTeamProjectCollection connection,
        final boolean createWorkspaceIfNone,
        final boolean workspaceRequired,
        final String title) {
        super(parentShell);

        Check.notNull(connection, "connection"); //$NON-NLS-1$
        Check.notNull(title, "title"); //$NON-NLS-1$

        this.connection = connection;
        this.createWorkspaceIfNone = createWorkspaceIfNone;
        this.workspaceRequired = workspaceRequired;
        this.title = title;
    }

    public Workspace getSelectedWorkspace() {
        return control.getWorkspacesTable().getSelectedWorkspace();
    }

    public WorkspacesControl getWorkspacesControl() {
        if (control == null) {
            create();
        }

        return control;
    }

    @Override
    protected void hookAddToDialogArea(final Composite dialogArea) {
        final GridLayout layout = new GridLayout();
        layout.marginWidth = getHorizontalMargin();
        layout.marginWidth = getVerticalMargin();
        layout.horizontalSpacing = getHorizontalSpacing();
        layout.verticalSpacing = getVerticalSpacing();
        dialogArea.setLayout(layout);

        SWTUtil.createLabel(dialogArea, Messages.getString("WorkspacesDialog.WorkspacesLabelText")); //$NON-NLS-1$

        control = new WorkspacesControl(dialogArea, SWT.NONE);
        GridDataBuilder.newInstance().grab().fill().applyTo(control);
        control.getWorkspacesTable().addDoubleClickListener(new IDoubleClickListener() {
            @Override
            public void doubleClick(final DoubleClickEvent event) {
                WorkspacesDialog.this.onWorkspacesTableDoubleClick(event);
            }
        });
        control.getWorkspacesTable().addTraverseListener(new TraverseListener() {
            @Override
            public void keyTraversed(final TraverseEvent e) {
                WorkspacesDialog.this.onWorkspacesTableKeyTraversed(e);
            }
        });

        control.refresh(connection, createWorkspaceIfNone, true);
    }

    @Override
    protected String provideDialogTitle() {
        return title;
    }

    @Override
    protected void buttonPressed(final int buttonId) {
        if (IDialogConstants.CLOSE_ID == buttonId) {
            setReturnCode(buttonId);
            close();
        } else {
            super.buttonPressed(buttonId);
        }
    }

    @Override
    protected void createButtonsForButtonBar(final Composite parent) {
        if (workspaceRequired) {
            final Button okButton = createButton(
                parent,
                IDialogConstants.OK_ID,
                Messages.getString("WorkspacesDialog.UseWorkspaceButtonText"), //$NON-NLS-1$
                true);
            createButton(parent, IDialogConstants.CLOSE_ID, IDialogConstants.CLOSE_LABEL, false);
            new ButtonValidatorBinding(okButton).bind(control);
        } else {
            createButton(parent, IDialogConstants.CLOSE_ID, IDialogConstants.CLOSE_LABEL, true);
        }
    }

    private void onWorkspacesTableDoubleClick(final DoubleClickEvent event) {
        if (workspaceRequired) {
            okPressed();
        } else {
            control.editSelectedWorkspace();
        }
    }

    private void onWorkspacesTableKeyTraversed(final TraverseEvent e) {
        if (workspaceRequired && e.detail == SWT.TRAVERSE_RETURN) {
            control.editSelectedWorkspace();
            e.doit = false;
        }
    }
}
