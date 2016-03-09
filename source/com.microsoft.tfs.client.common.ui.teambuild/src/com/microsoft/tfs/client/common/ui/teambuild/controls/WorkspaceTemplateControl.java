// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teambuild.controls;

import java.util.Arrays;
import java.util.Comparator;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

import com.microsoft.tfs.client.common.ui.controls.generic.BaseControl;
import com.microsoft.tfs.client.common.ui.controls.workspaces.WorkingFolderData;
import com.microsoft.tfs.client.common.ui.controls.workspaces.WorkingFolderDataEditEvent;
import com.microsoft.tfs.client.common.ui.controls.workspaces.WorkingFolderDataEditListener;
import com.microsoft.tfs.client.common.ui.controls.workspaces.WorkingFolderDataTable;
import com.microsoft.tfs.client.common.ui.framework.helper.ButtonHelper;
import com.microsoft.tfs.client.common.ui.framework.helper.SWTUtil;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.framework.sizing.ControlSize;
import com.microsoft.tfs.client.common.ui.teambuild.Messages;
import com.microsoft.tfs.client.common.ui.teambuild.VersionControlHelper;
import com.microsoft.tfs.client.common.ui.teambuild.dialogs.WorkspaceSelectionDialog;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.build.BuildConstants;
import com.microsoft.tfs.core.clients.build.IWorkspaceMapping;
import com.microsoft.tfs.core.clients.build.IWorkspaceTemplate;
import com.microsoft.tfs.core.clients.build.flags.WorkspaceMappingDepth;
import com.microsoft.tfs.core.clients.build.utils.BuildPath;
import com.microsoft.tfs.core.clients.versioncontrol.path.LocalPath;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.WorkingFolder;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.WorkingFolderType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;

public class WorkspaceTemplateControl extends BaseControl {
    private final TFSTeamProjectCollection connection;
    private WorkingFolderDataTable table;
    private Button copyButton;

    public WorkspaceTemplateControl(
        final Composite parent,
        final int style,
        final TFSTeamProjectCollection connection) {
        super(parent, style);
        this.connection = connection;
        createControls(this);
    }

    private void createControls(final Composite composite) {
        final GridLayout layout = SWTUtil.gridLayout(composite, 1);
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        layout.horizontalSpacing = getHorizontalSpacing();
        layout.verticalSpacing = getVerticalSpacing();

        SWTUtil.createLabel(composite, Messages.getString("WorkspaceTemplateControl.WorkingFoldersLabelText")); //$NON-NLS-1$

        table = new WorkingFolderDataTable(composite, SWT.FULL_SELECTION | SWT.MULTI, connection);
        GridDataBuilder.newInstance().fill().grab().applyTo(table);

        // TODO: Need to figure out why sizing of this has gone strange...
        ControlSize.setCharWidthHint(table, 10);

        copyButton = SWTUtil.createButton(composite, Messages.getString("WorkspaceTemplateControl.CopyButtonText")); //$NON-NLS-1$
        copyButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                copyWorkspacePressed();
            }
        });
        ButtonHelper.setButtonToButtonBarSize(copyButton);

        table.addEditListener(new WorkingFolderDataEditListener() {
            @Override
            public void onWorkingFolderDataEdit(final WorkingFolderDataEditEvent event) {
                if (event.getColumnPropertyName().equals(WorkingFolderDataTable.COLUMN_SERVER)) {
                    final WorkingFolderData folderData = event.getWorkingFolder();
                    if (folderData.getType() == WorkingFolderType.CLOAK) {
                        return;
                    }

                    // Default the local path if none already set and the server
                    // path is not empty.
                    if ((folderData.getLocalItem() == null || folderData.getLocalItem().trim().length() == 0)
                        && folderData.getServerItem() != null
                        && folderData.getServerItem().trim().length() >= 0) {
                        folderData.setLocalItem(calculateDefaultLocalPath(folderData.getServerItem()));
                    }
                    return;
                }
                if (event.getColumnPropertyName().equals(WorkingFolderDataTable.COLUMN_LOCAL)
                    && event.getWorkingFolder().getLocalItem() != null) {
                    final WorkingFolderData folderData = event.getWorkingFolder();
                    if (folderData.getLocalItem().equals(LocalPath.nativeToTFS(folderData.getLocalItem()))) {
                        // If path entered is a windows style path, just use it.
                        return;
                    }
                    // Convert to windows style path.
                    String localPath = folderData.getLocalItem();
                    if (!localPath.substring(1, 2).equals(":")) //$NON-NLS-1$
                    {
                        localPath = LocalPath.nativeToTFS(localPath);
                        if (localPath.startsWith("U:")) //$NON-NLS-1$
                        {
                            if (localPath.startsWith("U:$(SourceDir)")) //$NON-NLS-1$
                            {
                                localPath = localPath.substring(2);
                            } else {
                                // Convert path to C:\ from U:\ as that is more
                                // like
                                // what would be expected.
                                localPath = "C" + localPath.substring(1); //$NON-NLS-1$
                            }
                        }
                    }
                    folderData.setLocalItem(localPath);
                }

            }
        });
    }

    protected void copyWorkspacePressed() {
        // Open up workspace selection dialog.
        final WorkspaceSelectionDialog dialog = new WorkspaceSelectionDialog(getShell(), connection);
        if (dialog.open() == IDialogConstants.OK_ID) {
            final Workspace selectedWorkspace = dialog.getSelectedWorkspace();
            if (selectedWorkspace == null || selectedWorkspace.getFolders() == null) {
                return;
            }

            table.setWorkingFolderDataCollection(
                VersionControlHelper.rerootToBuildDirLocalItems(
                    selectedWorkspace.getFolders(),
                    connection.getVersionControlClient()));

        }
    }

    public WorkingFolderDataTable getTable() {
        return table;
    }

    public Button getCopyButton() {
        return copyButton;
    }

    // Note equivalent to
    // Microsoft.TeamFoundation.Build.Controls.DialogBuildDefinition+
    // WorkspaceNeededEventHandler#RerootToBuildDirServerItems(string path)
    protected String calculateDefaultLocalPath(final String serverItem) {
        String path = serverItem.replace('/', BuildPath.PATH_SEPERATOR_CHAR);
        path = path.replace('\\', BuildPath.PATH_SEPERATOR_CHAR);

        int seperatorPos = path.indexOf(BuildPath.PATH_SEPERATOR_CHAR);
        if (seperatorPos >= 0) {
            seperatorPos = path.indexOf(BuildPath.PATH_SEPERATOR_CHAR, seperatorPos + 1);
            if (seperatorPos < 0) {
                path = ""; //$NON-NLS-1$
            }
        }
        if (seperatorPos >= 0) {
            path = path.substring(seperatorPos + 1);
        }

        if (path.length() == 0) {
            path = BuildConstants.SOURCE_DIR_ENVIRONMENT_VARIABLE;
        } else {
            path = BuildConstants.SOURCE_DIR_ENVIRONMENT_VARIABLE + LocalPath.TFS_PREFERRED_LOCAL_PATH_SEPARATOR + path;
        }

        return path;
    }

    private WorkingFolderData[] convertWorkingFolders(final IWorkspaceTemplate template) {
        final IWorkspaceMapping[] mappings = template.getMappings();
        final WorkingFolderData[] folderDatas = new WorkingFolderData[mappings.length];
        for (int i = 0; i < mappings.length; i++) {
            String serverPath;
            if (mappings[i].getDepth().equals(WorkspaceMappingDepth.ONE_LEVEL)) {
                serverPath = ServerPath.combine(mappings[i].getServerItem(), WorkingFolder.DEPTH_ONE_STRING);
            } else {
                serverPath = mappings[i].getServerItem();
            }
            folderDatas[i] = new WorkingFolderData(
                serverPath,
                mappings[i].getLocalItem(),
                mappings[i].getMappingType().getWorkingFolderType());
        }

        // Before we return them - sort into ServerPath order.
        Arrays.sort(folderDatas, new Comparator<WorkingFolderData>() {
            @Override
            public int compare(final WorkingFolderData x, final WorkingFolderData y) {
                return ServerPath.compareTopDown(x.getServerItem(), y.getServerItem());
            }
        });

        return folderDatas;
    }

    public void setWorkspaceTemplate(final IWorkspaceTemplate template) {
        getTable().setWorkingFolders(convertWorkingFolders(template));
    }

}
