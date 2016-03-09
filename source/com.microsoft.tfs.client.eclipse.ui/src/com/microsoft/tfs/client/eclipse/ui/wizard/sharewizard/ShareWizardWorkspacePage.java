// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.ui.wizard.sharewizard;

import java.util.Map;

import org.eclipse.core.resources.IProject;

import com.microsoft.tfs.client.common.ui.wizard.common.WizardWorkspacePage;
import com.microsoft.tfs.client.eclipse.ui.Messages;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.WorkingFolder;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;

/**
 * Overrides the common wizard workspace page to query working folder mappings
 * for the selected projects.
 *
 * This sets the page data {@link WorkingFolder.class} with a map keyed on
 * IProject with a value of the WorkingFolder mapping appropriate for that
 * project. All, some or none of the IProjects may have a corresponding
 * WorkingFolder.
 */
public class ShareWizardWorkspacePage extends WizardWorkspacePage {
    public static final String PAGE_NAME = "ShareWizardWorkspacePage"; //$NON-NLS-1$

    public static final String PROJECT_WORKING_FOLDER_MAP = "ShareWizardWorkspacePage.projectWorkingFolderMap"; //$NON-NLS-1$

    public ShareWizardWorkspacePage() {
        super(
            PAGE_NAME,
            Messages.getString("ShareWizardWorkspacePage.PageName"), //$NON-NLS-1$
            Messages.getString("ShareWizardWorkspacePage.PageDescription")); //$NON-NLS-1$

        setText(Messages.getString("ShareWizardWorkspacePage.PageText")); //$NON-NLS-1$
    }

    @Override
    protected void refresh() {
        super.refresh();

        getExtendedWizard().removePageData(WorkingFolder.class);
    }

    @Override
    protected boolean onPageFinished() {
        if (!super.onPageFinished()) {
            return false;
        }

        final Workspace workspace = getSelectedWorkspace();
        final Map<IProject, String> projectWorkingFolderMap =
            ((ShareWizard) getExtendedWizard()).createProjectWorkingFolderMap(workspace);

        getExtendedWizard().setPageData(PROJECT_WORKING_FOLDER_MAP, projectWorkingFolderMap);

        return true;
    }
}
