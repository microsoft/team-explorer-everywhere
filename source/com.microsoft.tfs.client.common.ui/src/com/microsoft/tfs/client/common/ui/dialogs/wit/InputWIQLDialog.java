// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.dialogs.wit;

import java.text.MessageFormat;
import java.util.Arrays;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.microsoft.tfs.client.common.server.TFSServer;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.framework.dialog.BaseDialog;
import com.microsoft.tfs.client.common.ui.framework.helper.MessageBoxHelpers;
import com.microsoft.tfs.core.clients.commonstructure.ProjectInfo;
import com.microsoft.tfs.core.clients.workitem.WorkItemClient;
import com.microsoft.tfs.core.clients.workitem.project.Project;
import com.microsoft.tfs.core.clients.workitem.project.ProjectCollection;

public class InputWIQLDialog extends BaseDialog {
    private Text wiqlText;
    private Combo projectCombo;
    private final Project[] projects;
    private String wiql;
    private Project selectedProject;
    private final WorkItemClient workItemClient;

    public InputWIQLDialog(
        final Shell parentShell,
        final TFSServer server,
        final ProjectCollection projectCollection,
        final Project initialProject,
        final String initialWiql) {
        super(parentShell);
        workItemClient = projectCollection.getClient();
        selectedProject = initialProject;
        wiql = initialWiql;

        final ProjectInfo[] activeProjects = server.getProjectCache().getActiveTeamProjects();

        projects = new Project[activeProjects.length];
        for (int i = 0; i < activeProjects.length; i++) {
            projects[i] = projectCollection.get(activeProjects[i].getName());
        }
        Arrays.sort(projects);
    }

    @Override
    protected void hookAddToDialogArea(final Composite dialogArea) {
        final GridLayout layout = new GridLayout(2, false);
        layout.marginWidth = getHorizontalMargin();
        layout.marginHeight = getVerticalMargin();
        layout.horizontalSpacing = getHorizontalSpacing();
        layout.verticalSpacing = getVerticalSpacing();
        dialogArea.setLayout(layout);

        final Label projectLabel = new Label(dialogArea, SWT.NONE);
        projectLabel.setText(Messages.getString("InputWiqlDialog.projectLabelText")); //$NON-NLS-1$

        projectCombo = new Combo(dialogArea, SWT.READ_ONLY);

        final Label wiqlLabel = new Label(dialogArea, SWT.NONE);
        wiqlLabel.setText(Messages.getString("InputWiqlDialog.EnterWiqlLabelText")); //$NON-NLS-1$
        GridData gd = new GridData();
        gd.horizontalSpan = 2;
        wiqlLabel.setLayoutData(gd);

        wiqlText = new Text(dialogArea, SWT.BORDER | SWT.MULTI | SWT.WRAP);
        gd = new GridData();
        gd.horizontalAlignment = SWT.FILL;
        gd.grabExcessHorizontalSpace = true;
        gd.verticalAlignment = SWT.FILL;
        gd.grabExcessVerticalSpace = true;
        gd.horizontalSpan = 2;
        wiqlText.setLayoutData(gd);

        initialPopulate();
    }

    private void initialPopulate() {
        if (wiql != null) {
            wiqlText.setText(wiql);
        }

        for (int i = 0; i < projects.length; i++) {
            projectCombo.add(projects[i].getName());
            if (projects[i] == selectedProject) {
                projectCombo.select(i);
            }
        }

        if (selectedProject == null && projects.length > 0) {
            projectCombo.select(0);
            selectedProject = projects[0];
        }

        if (projects.length == 0) {
            projectCombo.setEnabled(false);
        }

        wiqlText.setFocus();
        wiqlText.selectAll();
    }

    @Override
    protected void okPressed() {
        wiql = wiqlText.getText();
        if (projectCombo.getSelectionIndex() != -1) {
            selectedProject = projects[projectCombo.getSelectionIndex()];
        }

        try {
            workItemClient.validateWIQL(wiql);
        } catch (final Throwable e) {
            final String messageFormat = Messages.getString("InputWiqlDialog.ErrorParsingWiqlFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, e.getMessage());
            MessageBoxHelpers.errorMessageBox(
                getShell(),
                Messages.getString("InputWiqlDialog.ErrorDialogTitle"), //$NON-NLS-1$
                message);

            wiqlText.setFocus();
            wiqlText.selectAll();
            return;
        }

        super.okPressed();
    }

    public String getWIQL() {
        return wiql;
    }

    public Project getSelectedProject() {
        return selectedProject;
    }

    @Override
    protected Point getInitialSize() {
        return new Point(500, 375);
    }

    @Override
    protected String provideDialogTitle() {
        return Messages.getString("InputWiqlDialog.EnterWiqlDialogTitle"); //$NON-NLS-1$
    }
}
