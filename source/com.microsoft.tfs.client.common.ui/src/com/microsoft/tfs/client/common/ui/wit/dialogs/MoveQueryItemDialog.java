// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.wit.dialogs;

import java.text.MessageFormat;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.microsoft.tfs.client.common.server.TFSServer;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.framework.dialog.BaseDialog;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.framework.sizing.ControlSize;
import com.microsoft.tfs.client.common.ui.wit.controls.QueryItemTreeControl;
import com.microsoft.tfs.core.clients.workitem.project.Project;
import com.microsoft.tfs.core.clients.workitem.queryhierarchy.QueryFolder;
import com.microsoft.tfs.core.clients.workitem.queryhierarchy.QueryItem;
import com.microsoft.tfs.core.clients.workitem.queryhierarchy.QueryItemType;
import com.microsoft.tfs.util.Check;

public class MoveQueryItemDialog extends BaseDialog {
    private final TFSServer server;

    private QueryItemTreeControl queryItemTreeControl;
    private Text nameText;
    private final Project[] projects;
    private final QueryItem queryItem;
    private final QueryItemType type;

    private String name;
    private QueryFolder parent;

    private String prompt = Messages.getString("MoveQueryItemDialog.DefaultPrompt"); //$NON-NLS-1$

    public MoveQueryItemDialog(
        final Shell parentShell,
        final TFSServer server,
        final Project[] projects,
        final QueryItem queryItem) {
        super(parentShell);

        Check.notNull(server, "server"); //$NON-NLS-1$
        Check.notNull(projects, "projects"); //$NON-NLS-1$
        Check.notNull(queryItem, "queryItem"); //$NON-NLS-1$

        this.server = server;
        this.projects = projects;
        this.queryItem = queryItem;
        type = QueryItemType.QUERY_FOLDER;

        name = queryItem.getName();
        parent = queryItem.getParent();
    }

    public QueryItem getSelectedQueryItem() {
        return queryItemTreeControl.getSelectedQueryItem();
    }

    @Override
    protected void hookAddToDialogArea(final Composite dialogArea) {
        // ContextSensitiveHelp.setHelp(dialogArea.getParent(),
        // TFSUIHelpContextIDs.WORKITEM_SELECT_QUERY);

        final GridLayout layout = new GridLayout(2, false);
        layout.marginWidth = getHorizontalMargin();
        layout.marginHeight = getVerticalMargin();
        layout.horizontalSpacing = getHorizontalSpacing();
        layout.verticalSpacing = getVerticalSpacing();
        dialogArea.setLayout(layout);

        final Label label = new Label(dialogArea, SWT.NONE);

        final String messageFormat = Messages.getString("MoveQueryItemDialog.PromptLabelTextFormat"); //$NON-NLS-1$
        final String message = MessageFormat.format(messageFormat, getPrompt());
        label.setText(message);

        queryItemTreeControl =
            new QueryItemTreeControl(dialogArea, SWT.NONE, server, projects, queryItem.getParent(), type);

        queryItemTreeControl.addQueryItemDoubleClickedListener(new QueryItemDoubleClickedListener());
        queryItemTreeControl.addQueryItemSelectionListener(new QueryItemSelectionListener());

        queryItemTreeControl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));

        final Label nameLabel = new Label(dialogArea, SWT.NONE);
        nameLabel.setText(Messages.getString("MoveQueryItemDialog.NameLabelText")); //$NON-NLS-1$

        nameText = new Text(dialogArea, SWT.BORDER);
        nameText.setText(queryItem.getName());
        GridDataBuilder.newInstance().hGrab().hFill().applyTo(nameText);
        nameText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(final ModifyEvent e) {
                getButton(IDialogConstants.OK_ID).setEnabled(nameText.getText().length() > 0);
            }
        });

        ControlSize.setCharHeightHint(queryItemTreeControl, 20);
        ControlSize.setCharWidthHint(queryItemTreeControl, 100);
    }

    @Override
    protected void hookDialogAboutToClose() {
        name = nameText.getText();
        parent = (QueryFolder) queryItemTreeControl.getSelectedQueryItem();
    }

    public void setPrompt(final String prompt) {
        this.prompt = prompt;
    }

    public String getPrompt() {
        return prompt;
    }

    public String getName() {
        return name;
    }

    public QueryFolder getParent() {
        return parent;
    }

    @Override
    protected String provideDialogTitle() {
        return getPrompt();
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
