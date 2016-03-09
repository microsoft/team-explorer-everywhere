// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.wit.dialogs;

import java.text.MessageFormat;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.server.TFSServer;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.framework.dialog.BaseDialog;
import com.microsoft.tfs.client.common.ui.framework.sizing.ControlSize;
import com.microsoft.tfs.client.common.ui.wit.controls.QueryItemTreeControl;
import com.microsoft.tfs.core.clients.commonstructure.internal.ProjectInfoHelper;
import com.microsoft.tfs.core.clients.workitem.project.Project;
import com.microsoft.tfs.core.clients.workitem.queryhierarchy.QueryItem;
import com.microsoft.tfs.core.clients.workitem.queryhierarchy.QueryItemType;

public class SelectQueryItemDialog extends BaseDialog {
    private QueryItemTreeControl queryItemTreeControl;

    private final Project[] projects;
    private final String[] activeProjects;
    private final QueryItem initialQueryItem;
    private final QueryItemType type;

    private String prompt = Messages.getString("SelectQueryItemDialog.SelectQueryDialogTitle"); //$NON-NLS-1$

    public SelectQueryItemDialog(
        final Shell parentShell,
        final TFSServer server,
        final Project[] projects,
        final QueryItem initialQueryItem,
        final QueryItemType type) {
        this(
            parentShell,
            projects,
            ProjectInfoHelper.getProjectNames(server.getProjectCache().getActiveTeamProjects()),
            initialQueryItem,
            type);
    }

    public SelectQueryItemDialog(
        final Shell parentShell,
        final Project[] projects,
        final String[] activeProjects,
        final QueryItem initialQueryItem,
        final QueryItemType type) {
        super(parentShell);

        this.projects = projects;
        this.activeProjects = activeProjects;
        this.initialQueryItem = initialQueryItem;
        this.type = type;

        if (type != null && type.contains(QueryItemType.QUERY_DEFINITION)) {
            prompt = Messages.getString("SelectQueryItemDialog.SelectQueryDialogTitle"); //$NON-NLS-1$
        } else {
            prompt = Messages.getString("SelectQueryItemDialog.SelectQueryFolderDialogTitle"); //$NON-NLS-1$
        }
    }

    public QueryItem getSelectedQueryItem() {
        return queryItemTreeControl.getSelectedQueryItem();
    }

    @Override
    protected void hookAddToDialogArea(final Composite dialogArea) {
        // ContextSensitiveHelp.setHelp(dialogArea.getParent(),
        // TFSUIHelpContextIDs.WORKITEM_SELECT_QUERY);

        final GridLayout layout = new GridLayout(1, true);
        layout.marginWidth = getHorizontalMargin();
        layout.marginHeight = getVerticalMargin();
        layout.horizontalSpacing = getHorizontalSpacing();
        layout.verticalSpacing = getVerticalSpacing();
        dialogArea.setLayout(layout);

        final Label label = new Label(dialogArea, SWT.NONE);

        final String messageFormat = Messages.getString("SelectQueryItemDialog.PromptLabelTextFormat"); //$NON-NLS-1$
        final String message = MessageFormat.format(messageFormat, getPrompt());
        label.setText(message);

        queryItemTreeControl =
            new QueryItemTreeControl(dialogArea, SWT.NONE, projects, activeProjects, initialQueryItem, type);

        queryItemTreeControl.addQueryItemDoubleClickedListener(new QueryItemDoubleClickedListener());
        queryItemTreeControl.addQueryItemSelectionListener(new QueryItemSelectionListener());

        queryItemTreeControl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));

        ControlSize.setCharHeightHint(queryItemTreeControl, 20);
        ControlSize.setCharWidthHint(queryItemTreeControl, 100);
    }

    public void setPrompt(final String prompt) {
        this.prompt = prompt;
    }

    public String getPrompt() {
        return prompt;
    }

    @Override
    protected String provideDialogTitle() {
        return getPrompt();
    }

    @Override
    protected void hookAfterButtonsCreated() {
        getButton(IDialogConstants.OK_ID).setEnabled(initialQueryItem != null);
    }

    private class QueryItemDoubleClickedListener implements QueryItemTreeControl.QueryItemDoubleClickedListener {
        @Override
        public void queryItemDoubleClicked(final QueryItem queryItem) {
            okPressed();
        }
    }

    private class QueryItemSelectionListener implements QueryItemTreeControl.QueryItemSelectionListener {
        @Override
        public void queryItemSelected(final QueryItem queryItem) {
            getButton(IDialogConstants.OK_ID).setEnabled(queryItem != null);
        }
    }
}
