// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teambuild.dialogs;

import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.microsoft.tfs.client.common.ui.framework.command.ProgressMonitorDialogCommandExecutor;
import com.microsoft.tfs.client.common.ui.framework.dialog.BaseDialog;
import com.microsoft.tfs.client.common.ui.framework.helper.ButtonHelper;
import com.microsoft.tfs.client.common.ui.framework.helper.SWTUtil;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.framework.sizing.ControlSize;
import com.microsoft.tfs.client.common.ui.framework.table.TableColumnData;
import com.microsoft.tfs.client.common.ui.framework.table.TableControl;
import com.microsoft.tfs.client.common.ui.teambuild.Messages;
import com.microsoft.tfs.client.common.ui.teambuild.commands.FindWorkspacesCommand;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;

public class WorkspaceSelectionDialog extends BaseDialog {

    private Text ownerText;
    private WorkspaceSelectionTableControl table;
    private final TFSTeamProjectCollection connection;

    private Workspace selectedWorkspace = null;

    public WorkspaceSelectionDialog(final Shell parentShell, final TFSTeamProjectCollection connection) {
        super(parentShell);
        this.connection = connection;
    }

    @Override
    protected void hookAddToDialogArea(final Composite composite) {
        final GridLayout layout = SWTUtil.gridLayout(composite, 2, false);
        layout.marginWidth = getHorizontalMargin();
        layout.marginHeight = getVerticalMargin();
        layout.horizontalSpacing = getHorizontalSpacing();
        layout.verticalSpacing = getVerticalSpacing();

        final Label ownerLabel =
            SWTUtil.createLabel(composite, Messages.getString("WorkspaceSelectionDialog.OwnerLabelText")); //$NON-NLS-1$
        GridDataBuilder.newInstance().hSpan(layout).applyTo(ownerLabel);

        ownerText = new Text(composite, SWT.BORDER);
        GridDataBuilder.newInstance().hGrab().hFill().applyTo(ownerText);
        ownerText.addTraverseListener(new TraverseListener() {
            @Override
            public void keyTraversed(final TraverseEvent e) {
                find();
            }
        });
        ownerText.setText(connection.getAuthorizedAccountName());

        final Button findButton =
            SWTUtil.createButton(composite, Messages.getString("WorkspaceSelectionDialog.FindButtonText")); //$NON-NLS-1$
        GridDataBuilder.newInstance().vAlign(SWT.CENTER).applyTo(findButton);
        findButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                find();
            }
        });
        ButtonHelper.setButtonToButtonBarSize(findButton);

        final Label workspaceLabel =
            SWTUtil.createLabel(composite, Messages.getString("WorkspaceSelectionDialog.WorkspaceLabelText")); //$NON-NLS-1$
        GridDataBuilder.newInstance().hSpan(layout).applyTo(workspaceLabel);

        table = new WorkspaceSelectionTableControl(composite);
        table.addDoubleClickListener(new IDoubleClickListener() {
            @Override
            public void doubleClick(final DoubleClickEvent event) {
                okPressed();
            }
        });
        GridDataBuilder.newInstance().hSpan(layout).fill().grab().wHint(getMinimumMessageAreaWidth()).applyTo(table);
        ControlSize.setCharHeightHint(table, 10);
        find();
        table.setFocus();
    }

    @Override
    protected void hookDialogAboutToClose() {
        selectedWorkspace = table.getSelectedWorkspace();
    }

    protected void find() {
        final String owner = ownerText.getText().trim();
        if (owner.length() == 0) {
            return;
        }

        final FindWorkspacesCommand command = new FindWorkspacesCommand(connection.getVersionControlClient(), owner);
        final ProgressMonitorDialogCommandExecutor executor = new ProgressMonitorDialogCommandExecutor(getShell());
        executor.execute(command);

        if (!table.isDisposed() && command.getWorkspaces() != null) {
            table.setWorkspaces(command.getWorkspaces());
            table.setSelectedElement(table.getElement(0));
        }

    }

    /*
     * (non-Javadoc)
     *
     * @seecom.microsoft.tfs.client.common.ui.shared.dialog.BaseDialog#
     * provideDialogTitle()
     */
    @Override
    protected String provideDialogTitle() {
        return Messages.getString("WorkspaceSelectionDialog.SelectWorkspaceDialogTitle"); //$NON-NLS-1$
    }

    private class WorkspaceSelectionTableControl extends TableControl {
        public static final String COLUMN_OWNER = "owner"; //$NON-NLS-1$
        public static final String COLUMN_COMPUTER = "computer"; //$NON-NLS-1$
        public static final String COLUMN_NAME = "name"; //$NON-NLS-1$

        protected WorkspaceSelectionTableControl(final Composite parent) {
            super(parent, SWT.FULL_SELECTION | SWT.SINGLE, Workspace.class, null);

            final TableColumnData[] columnData = new TableColumnData[] {
                new TableColumnData(
                    Messages.getString("WorkspaceSelectionDialog.NameColumnText"), //$NON-NLS-1$
                    150,
                    0.4F,
                    COLUMN_NAME),
                new TableColumnData(
                    Messages.getString("WorkspaceSelectionDialog.ComputerColumnText"), //$NON-NLS-1$
                    100,
                    0.3F,
                    COLUMN_COMPUTER),
                new TableColumnData(
                    Messages.getString("WorkspaceSelectionDialog.OwnerColumnText"), //$NON-NLS-1$
                    100,
                    0.3F,
                    COLUMN_OWNER)
            };

            setOptionPersistGeometry(false);
            setupTable(true, false, columnData);

            setUseViewerDefaults();
        }

        @Override
        protected String getColumnText(final Object element, final String columnPropertyName) {
            final Workspace workspace = (Workspace) element;

            if (COLUMN_NAME.equals(columnPropertyName)) {
                return workspace.getName();
            }

            if (COLUMN_COMPUTER.equals(columnPropertyName)) {
                return workspace.getComputer();
            }

            if (COLUMN_OWNER.equals(columnPropertyName)) {
                return workspace.getOwnerName();
            }

            return ""; //$NON-NLS-1$
        }

        public void setWorkspaces(final Workspace[] workspaces) {
            setElements(workspaces);
        }

        public Workspace getSelectedWorkspace() {
            return (Workspace) getSelectedElement();
        }
    }

    public Workspace getSelectedWorkspace() {
        return selectedWorkspace;
    }
}
