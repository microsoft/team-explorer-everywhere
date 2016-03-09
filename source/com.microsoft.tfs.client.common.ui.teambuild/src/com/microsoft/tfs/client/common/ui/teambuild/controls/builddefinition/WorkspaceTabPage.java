// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teambuild.controls.builddefinition;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import com.microsoft.tfs.client.common.ui.controls.workspaces.WorkingFolderData;
import com.microsoft.tfs.client.common.ui.teambuild.Messages;
import com.microsoft.tfs.client.common.ui.teambuild.controls.WorkspaceTemplateControl;
import com.microsoft.tfs.core.clients.build.BuildConstants;
import com.microsoft.tfs.core.clients.build.IBuildDefinition;
import com.microsoft.tfs.core.clients.build.IWorkspaceTemplate;
import com.microsoft.tfs.core.clients.build.flags.WorkspaceMappingDepth;
import com.microsoft.tfs.core.clients.build.soapextensions.WorkspaceMappingType;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.WorkingFolder;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.WorkingFolderType;

public class WorkspaceTabPage extends BuildDefinitionTabPage {

    private WorkspaceTemplateControl control;

    public WorkspaceTabPage(final IBuildDefinition buildDefinition) {
        super(buildDefinition);
    }

    @Override
    public Control createControl(final Composite parent) {
        control = new WorkspaceTemplateControl(parent, SWT.NONE, getBuildDefinition().getBuildServer().getConnection());
        populateControl();
        return control;
    }

    private void populateControl() {
        if (getBuildDefinition().getWorkspace().getMappings().length == 0) {
            getBuildDefinition().getWorkspace().map(
                ServerPath.ROOT + getBuildDefinition().getTeamProject(),
                BuildConstants.SOURCE_DIR_ENVIRONMENT_VARIABLE);
        }

        control.setWorkspaceTemplate(getBuildDefinition().getWorkspace());

    }

    @Override
    public String getName() {
        return Messages.getString("WorkspaceTabPage.TabLabelText"); //$NON-NLS-1$
    }

    @Override
    public boolean isValid() {
        if (control == null) {
            return false;
        }

        return control.getTable().getWorkingFolders().length > 0;
    }

    public WorkspaceTemplateControl getControl() {
        return control;
    }

    public void updateSourceSettings(final IBuildDefinition buildDefinition) {
        final IWorkspaceTemplate template = buildDefinition.getWorkspace();
        template.clearMappings();

        final WorkingFolderData[] workingFolderData = getControl().getTable().getWorkingFolders();
        for (int i = 0; i < workingFolderData.length; i++) {
            // TODO: What should we do about native local paths here?

            final WorkingFolder workingFolder = workingFolderData[i].createWorkingFolder();
            buildDefinition.getWorkspace().addMapping(
                workingFolder.getServerItem(),
                workingFolderData[i].getLocalItem(),
                (workingFolder.getType() == WorkingFolderType.MAP) ? WorkspaceMappingType.MAP
                    : WorkspaceMappingType.CLOAK,
                (workingFolder.getDepth() == RecursionType.ONE_LEVEL) ? WorkspaceMappingDepth.ONE_LEVEL
                    : WorkspaceMappingDepth.FULL);
        }
    }
}
