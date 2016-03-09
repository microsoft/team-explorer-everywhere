// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.workspaces;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.controls.generic.BaseControl;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.core.TFSTeamProjectCollection;

public class WorkspaceEditControl extends BaseControl {
    private final WorkspaceDetailsControl workspaceDetailsControl;
    private final WorkingFolderDataTable workingFolderDataTable;

    private WorkspaceData workspaceData;

    public WorkspaceEditControl(final Composite parent, final int style) {
        this(parent, style, null);
    }

    public WorkspaceEditControl(final Composite parent, final int style, final TFSTeamProjectCollection connection) {
        super(parent, style);

        final GridLayout layout = new GridLayout();
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        layout.horizontalSpacing = getHorizontalSpacing();
        layout.verticalSpacing = getVerticalSpacing();
        setLayout(layout);

        workspaceDetailsControl = new WorkspaceDetailsControl(this, SWT.NONE, connection);
        GridDataBuilder.newInstance().hGrab().hFill().applyTo(workspaceDetailsControl);

        final Label workingFoldersLabel = new Label(this, SWT.NONE);
        workingFoldersLabel.setText(Messages.getString("WorkspaceEditControl.WorkingFoldersLabelText")); //$NON-NLS-1$
        GridDataBuilder.newInstance().hGrab().hFill().vIndent(getVerticalSpacing()).applyTo(workingFoldersLabel);

        workingFolderDataTable = new WorkingFolderDataTable(this, SWT.FULL_SELECTION | SWT.MULTI, connection);
        GridDataBuilder.newInstance().grab().fill().applyTo(workingFolderDataTable);
    }

    public void setAdvanced(final boolean advanced) {
        workspaceDetailsControl.setAdvanced(advanced);
    }

    public WorkspaceDetailsControl getWorkspaceDetailsControl() {
        return workspaceDetailsControl;
    }

    public WorkingFolderDataTable getWorkingFolderDataTable() {
        return workingFolderDataTable;
    }

    public void setConnection(final TFSTeamProjectCollection connection) {
        workspaceDetailsControl.setConnection(connection);
        workingFolderDataTable.setConnection(connection);
    }

    public WorkspaceData getWorkspaceData() {
        return workspaceData;
    }

    public void setWorkspaceData(final WorkspaceData workspaceData) {
        workspaceDetailsControl.setWorkspaceDetails(workspaceData.getWorkspaceDetails());
        workingFolderDataTable.setWorkingFolderDataCollection(workspaceData.getWorkingFolderDataCollection());
    }

    public void setImmutable(final boolean immutable) {
        workspaceDetailsControl.setImmutable(immutable);
    }
}
