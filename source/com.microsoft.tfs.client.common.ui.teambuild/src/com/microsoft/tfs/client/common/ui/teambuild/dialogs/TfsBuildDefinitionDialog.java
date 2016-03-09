// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teambuild.dialogs;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.controls.workspaces.WorkingFolderData;
import com.microsoft.tfs.client.common.ui.dialogs.vc.ServerItemTreeDialog;
import com.microsoft.tfs.client.common.ui.teambuild.Messages;
import com.microsoft.tfs.client.common.ui.teambuild.VersionControlHelper;
import com.microsoft.tfs.client.common.ui.teambuild.controls.builddefinition.BuildDefinitionTabPage;
import com.microsoft.tfs.client.common.ui.teambuild.controls.builddefinition.ProjectFileTabPage;
import com.microsoft.tfs.client.common.ui.teambuild.controls.builddefinition.WorkspaceTabPage;
import com.microsoft.tfs.client.common.ui.teambuild.wizards.CreateBuildConfigurationWizard;
import com.microsoft.tfs.client.common.ui.vc.serveritem.ServerItemSource;
import com.microsoft.tfs.client.common.ui.vc.serveritem.ServerItemType;
import com.microsoft.tfs.client.common.ui.vc.serveritem.VersionedItemSource;
import com.microsoft.tfs.core.clients.build.IBuildDefinition;
import com.microsoft.tfs.core.clients.build.IBuildServer;
import com.microsoft.tfs.util.Check;

/**
 * Show the edit TFVC build definition dialog.
 */
public class TfsBuildDefinitionDialog extends BuildDefinitionDialog {
    private WorkspaceTabPage sourceSettingsTabPage;
    private ProjectFileTabPage projectFileTabPage;

    private final IBuildServer buildServer;
    private final IBuildDefinition buildDefinition;

    public TfsBuildDefinitionDialog(final Shell parentShell, final IBuildDefinition buildDefinition) {
        super(parentShell, buildDefinition);
        Check.notNull(buildDefinition, "buildDefinition"); //$NON-NLS-1$

        this.buildDefinition = buildDefinition;
        this.buildServer = buildDefinition.getBuildServer();
    }

    @Override
    protected BuildDefinitionTabPage getSourceSettingsTabPage(final IBuildDefinition buildDefinition) {
        sourceSettingsTabPage = new WorkspaceTabPage(buildDefinition);
        return sourceSettingsTabPage;
    }

    @Override
    protected ProjectFileTabPage getProjectFileTabPage(final IBuildDefinition buildDefinition) {
        projectFileTabPage = new ProjectFileTabPage(buildDefinition);
        return projectFileTabPage;
    }

    @Override
    protected SelectionListener getBrowseButtonSelectionListener(final Shell shell) {
        return new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                String initialPath = projectFileTabPage.getConfigFolderText();
                if (initialPath.length() == 0) {
                    initialPath = null;
                }

                final ServerItemSource serverItemSource = new VersionedItemSource(buildServer.getConnection());
                final ServerItemTreeDialog dialog = new ServerItemTreeDialog(
                    projectFileTabPage.getControl().getShell(),
                    Messages.getString("BuildDefinitionDialog.BrowseDialogTitle"), //$NON-NLS-1$
                    initialPath,
                    serverItemSource,
                    ServerItemType.ALL_FOLDERS);

                if (IDialogConstants.OK_ID == dialog.open()) {
                    projectFileTabPage.getControl().getConfigFolderText().setText(dialog.getSelectedServerPath());
                    validate();
                }
            }
        };
    }

    @Override
    protected SelectionListener getCreateButtonSelectionListener(final Shell shell) {
        return new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                final CreateBuildConfigurationWizard wizard = new CreateBuildConfigurationWizard();

                // update the build definition to contain values as
                // currently defined, that way
                // information inside is available in the creation wizard.
                updateAndVerifyBuildDefinition(true);

                wizard.init(buildDefinition);

                final WizardDialog dialog = new WizardDialog(getShell(), wizard);
                final int rc = dialog.open();

                if (rc == IDialogConstants.OK_ID) {
                    checkForBuildFileExistence(true);
                    validate();
                }
            }
        };
    }

    @Override
    protected String getDefaultBuildFileLocation(final String newName) {
        return VersionControlHelper.calculateDefaultBuildFileLocation(buildDefinition.getTeamProject(), newName);
    }

    @Override
    protected boolean updateSourceSettingAndProjectFiles() {
        final WorkingFolderData[] data = sourceSettingsTabPage.getControl().getTable().getWorkingFolders();
        final List<IStatus> errorStatuses = new ArrayList<IStatus>(data.length);

        for (final WorkingFolderData d : data) {
            if (!versionControl.testItemExists(d.getServerItem())) {
                errorStatuses.add(
                    new Status(
                        Status.ERROR,
                        TFSCommonUIClientPlugin.PLUGIN_ID,
                        0,
                        MessageFormat.format(
                            Messages.getString("TfsBuildDefinitionDialog.InvalidSourceSettingMessageFormat"), //$NON-NLS-1$
                            d.getServerItem()),
                        null));
            }
        }

        if (errorStatuses.size() > 0) {
            final IStatus displayStatus = new MultiStatus(
                TFSCommonUIClientPlugin.PLUGIN_ID,
                0,
                errorStatuses.toArray(new IStatus[errorStatuses.size()]),
                Messages.getString("TfsBuildDefinitionDialog.ServerFoldersNotFound"), //$NON-NLS-1$
                null);

            ErrorDialog.openError(
                getShell(),
                Messages.getString("TfsBuildDefinitionDialog.InvalidSourceSetting"), //$NON-NLS-1$
                null,
                displayStatus);

            return false;
        } else {
            sourceSettingsTabPage.updateSourceSettings(buildDefinition);

            if (isMSBuildBasedBuild()) {
                projectFileTabPage.updateConfigurationFolderPath(
                    buildDefinition,
                    projectFileTabPage.getConfigFolderText());
            }

            return true;
        }
    }
}
