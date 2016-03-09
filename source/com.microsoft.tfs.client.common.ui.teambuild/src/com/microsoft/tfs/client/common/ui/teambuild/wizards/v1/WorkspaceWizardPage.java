// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teambuild.wizards.v1;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

import com.microsoft.tfs.client.common.ui.controls.workspaces.WorkingFolderData;
import com.microsoft.tfs.client.common.ui.framework.helper.SWTUtil;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.framework.validation.WizardPageValidatorBinding;
import com.microsoft.tfs.client.common.ui.teambuild.Messages;
import com.microsoft.tfs.client.common.ui.teambuild.controls.WorkspaceTemplateControl;
import com.microsoft.tfs.core.clients.build.IBuildDefinition;
import com.microsoft.tfs.core.clients.build.IWorkspaceTemplate;
import com.microsoft.tfs.core.clients.build.flags.WorkspaceMappingDepth;
import com.microsoft.tfs.core.clients.build.soapextensions.WorkspaceMappingType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.WorkingFolder;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.WorkingFolderType;

public class WorkspaceWizardPage extends WizardPage {

    private final IBuildDefinition buildDefinition;

    private WorkspaceTemplateControl workspaceTemplateControl;

    public WorkspaceWizardPage(final IBuildDefinition buildDefinition) {
        super("v1WorkspacePage", Messages.getString("WorkspaceWizardPage.PageTitle"), null); //$NON-NLS-1$ //$NON-NLS-2$
        setDescription(Messages.getString("WorkspaceWizardPage.PageDescription")); //$NON-NLS-1$
        this.buildDefinition = buildDefinition;
    }

    @Override
    public void createControl(final Composite parent) {
        final Composite composite = SWTUtil.createComposite(parent);
        SWTUtil.gridLayout(composite, 1);

        workspaceTemplateControl =
            new WorkspaceTemplateControl(composite, SWT.NONE, buildDefinition.getBuildServer().getConnection());
        GridDataBuilder.newInstance().fill().grab().applyTo(workspaceTemplateControl);

        workspaceTemplateControl.setWorkspaceTemplate(buildDefinition.getWorkspace());

        new WizardPageValidatorBinding(this).bind(workspaceTemplateControl.getTable().getElementsValidator());

        setControl(composite);
    }

    public IWorkspaceTemplate getWorkspaceTemplate() {
        // Workspace
        final IWorkspaceTemplate template = buildDefinition.getWorkspace();
        template.clearMappings();

        final WorkingFolderData[] workingFolders = workspaceTemplateControl.getTable().getWorkingFolders();
        for (int i = 0; i < workingFolders.length; i++) {
            final WorkingFolder workingFolder = workingFolders[i].createWorkingFolder();
            buildDefinition.getWorkspace().addMapping(
                workingFolder.getServerItem(),
                workingFolders[i].getLocalItem(),
                (workingFolder.getType() == WorkingFolderType.MAP) ? WorkspaceMappingType.MAP
                    : WorkspaceMappingType.CLOAK,
                (workingFolder.getDepth() == RecursionType.ONE_LEVEL) ? WorkspaceMappingDepth.ONE_LEVEL
                    : WorkspaceMappingDepth.FULL);
        }

        return buildDefinition.getWorkspace();
    }

}
