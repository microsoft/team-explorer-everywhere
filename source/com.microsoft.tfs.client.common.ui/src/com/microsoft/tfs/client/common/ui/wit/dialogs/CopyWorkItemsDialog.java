// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.wit.dialogs;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.framework.dialog.BaseDialog;
import com.microsoft.tfs.core.clients.workitem.WorkItemClient;
import com.microsoft.tfs.core.clients.workitem.project.Project;
import com.microsoft.tfs.core.clients.workitem.wittype.WorkItemType;

public class CopyWorkItemsDialog extends BaseDialog {
    private final WorkItemClient workItemClient;
    private Project[] projects;
    private WorkItemType[] workItemTypes;
    private Project project;
    private WorkItemType workItemType;

    private Combo projectCombo;
    private Combo workItemTypeCombo;

    public CopyWorkItemsDialog(
        final Shell parentShell,
        final WorkItemClient workItemClient,
        final Project initialProject,
        final WorkItemType initialWorkItemType) {
        super(parentShell);

        this.workItemClient = workItemClient;
        project = initialProject;
        workItemType = initialWorkItemType;
    }

    public Project getProject() {
        return project;
    }

    public WorkItemType getWorkItemType() {
        return workItemType;
    }

    @Override
    protected void hookAddToDialogArea(final Composite dialogArea) {
        final GridLayout gridLayout = new GridLayout(1, false);
        dialogArea.setLayout(gridLayout);

        final Label teamProjectLabel = new Label(dialogArea, SWT.NONE);
        teamProjectLabel.setText(Messages.getString("CopyWorkItemsDialog.TeamProjectLabelText")); //$NON-NLS-1$

        projectCombo = new Combo(dialogArea, SWT.DROP_DOWN | SWT.READ_ONLY);
        GridData gd = new GridData();
        gd.horizontalAlignment = SWT.FILL;
        gd.grabExcessHorizontalSpace = true;
        projectCombo.setLayoutData(gd);

        populateProjects();
        projectCombo.addSelectionListener(new ProjectChangedListener());

        final Label workItemTypeLabel = new Label(dialogArea, SWT.NONE);
        workItemTypeLabel.setText(Messages.getString("CopyWorkItemsDialog.TypeLabelText")); //$NON-NLS-1$

        workItemTypeCombo = new Combo(dialogArea, SWT.DROP_DOWN | SWT.READ_ONLY);
        gd = new GridData();
        gd.horizontalAlignment = SWT.FILL;
        gd.grabExcessHorizontalSpace = true;
        workItemTypeCombo.setLayoutData(gd);

        populateWorkItemTypes();
        workItemTypeCombo.addSelectionListener(new WorkItemTypeChangedListener());
    }

    private void populateProjects() {
        populateProjectArray();

        for (int i = 0; i < projects.length; i++) {
            projectCombo.add(projects[i].getName());
            if (projects[i] == project) {
                projectCombo.select(i);
            }
        }

        if (projectCombo.getSelectionIndex() == -1) {
            project = projects[0];
            projectCombo.select(0);
        }
    }

    private void populateProjectArray() {
        /*
         * Currently, MS implementation does not filter the project combo by
         * what projects are active in team explorer - each project on the
         * server is displayed
         *
         * This isn't the best for a number of reasons. If we ever want to
         * switch to more sane behavior, which would be to only show the
         * projects that are active in TE, use the commented block of code
         * instead.
         */

        /*
         * TEProject[] activeTeamProjects =
         * TeamExplorer.getInstance().getActiveProjects(false); projects = new
         * Project[activeTeamProjects.length]; for (int i = 0; i <
         * activeTeamProjects.length; i++) { projects[i] =
         * workItemClient.getProjects
         * ().getProjectByName(activeTeamProjects[i].getLabel()); }
         */

        projects = workItemClient.getProjects().getProjects();
    }

    private void populateWorkItemTypes() {
        workItemTypes = project.getVisibleWorkItemTypes();

        workItemTypeCombo.removeAll();

        for (int i = 0; i < workItemTypes.length; i++) {
            workItemTypeCombo.add(workItemTypes[i].getName());
            if (workItemTypes[i] == workItemType) {
                workItemTypeCombo.select(i);
            }
        }

        if (workItemTypeCombo.getSelectionIndex() == -1) {
            workItemType = workItemTypes[0];
            workItemTypeCombo.select(0);
        }
    }

    @Override
    protected String provideDialogTitle() {
        return Messages.getString("CopyWorkItemsDialog.DialogTitle"); //$NON-NLS-1$
    }

    private class WorkItemTypeChangedListener extends SelectionAdapter {
        @Override
        public void widgetSelected(final SelectionEvent e) {
            workItemType = workItemTypes[workItemTypeCombo.getSelectionIndex()];
        }
    }

    private class ProjectChangedListener extends SelectionAdapter {
        @Override
        public void widgetSelected(final SelectionEvent e) {
            project = projects[projectCombo.getSelectionIndex()];
            populateWorkItemTypes();
        }
    }
}
