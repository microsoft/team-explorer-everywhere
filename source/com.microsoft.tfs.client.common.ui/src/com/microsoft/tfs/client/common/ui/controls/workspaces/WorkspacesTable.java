// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.workspaces;

import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.framework.sizing.ControlSize;
import com.microsoft.tfs.client.common.ui.framework.table.EqualSizeTableLayout;
import com.microsoft.tfs.client.common.ui.framework.table.TableColumnData;
import com.microsoft.tfs.client.common.ui.framework.table.TableControl;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.util.NewlineUtils;

public class WorkspacesTable extends TableControl {
    public WorkspacesTable(final Composite parent, final int style) {
        this(parent, style, null);
    }

    public WorkspacesTable(final Composite parent, final int style, final String viewDataKey) {
        super(parent, style, Workspace.class, viewDataKey);

        final TableColumnData[] columnData = new TableColumnData[] {
            new TableColumnData(Messages.getString("WorkspacesTable.ColumnNameName"), 200, "name"), //$NON-NLS-1$ //$NON-NLS-2$
            new TableColumnData(Messages.getString("WorkspacesTable.ColumnNameComputer"), 200, "computer"), //$NON-NLS-1$ //$NON-NLS-2$
            new TableColumnData(Messages.getString("WorkspacesTable.Owner"), 200, "owner"), //$NON-NLS-1$//$NON-NLS-2$
            new TableColumnData(Messages.getString("WorkspacesTable.ColumnNameComment"), 300, "comment") //$NON-NLS-1$ //$NON-NLS-2$
        };
        setupTable(true, false, columnData);

        setUseViewerDefaults();

        getViewer().getTable().setLayout(new EqualSizeTableLayout());
    }

    public void setWorkspaces(final Workspace[] workspaces) {
        setElements(workspaces);
    }

    public Workspace[] getWorkspaces() {
        return (Workspace[]) getElements();
    }

    public void setSelectedWorkspaces(final Workspace[] workspaces) {
        setSelectedElements(workspaces);
    }

    public void setSelectedWorkspace(final Workspace workspace) {
        setSelectedElement(workspace);
    }

    public void setSelectedWorkspace(final String workspaceName) {
        if (workspaceName == null) {
            setSelectedElement(null);
            return;
        }

        final Workspace[] workspaces = getWorkspaces();
        for (int i = 0; i < workspaces.length; i++) {
            if (workspaces[i].getName().equals(workspaceName)) {
                setSelectedElement(workspaces[i]);
                return;
            }
        }
    }

    public Workspace[] getSelectedWorkspaces() {
        return (Workspace[]) getSelectedElements();
    }

    public Workspace getSelectedWorkspace() {
        return (Workspace) getSelectedElement();
    }

    public String getSelectedWorkspaceName() {
        final Workspace workspace = getSelectedWorkspace();
        return workspace == null ? null : workspace.getName();
    }

    public void setCheckedWorkspaces(final Workspace[] workspaces) {
        setCheckedElements(workspaces);
    }

    public Workspace[] getCheckedWorkspaces() {
        return (Workspace[]) getCheckedElements();
    }

    @Override
    public Point computeSize(final int wHint, final int hHint, final boolean changed) {
        return ControlSize.computeCharSize(wHint, hHint, getTable(), 100, 15);
    }

    @Override
    protected String getColumnText(final Object element, final int columnIndex) {
        final Workspace workspace = (Workspace) element;

        switch (columnIndex) {
            case 0:
                return workspace.getName();

            case 1:
                return workspace.getComputer();

            case 2:
                return workspace.getOwnerDisplayName();

            case 3:
                /*
                 * TODO: Microsoft also strips tabs and if the comment is
                 * greater than 120 chars, they limit it to 120 and append an
                 * ellipsis
                 */
                return NewlineUtils.stripNewlines(workspace.getComment());

            default:
                return ""; //$NON-NLS-1$
        }
    }
}
