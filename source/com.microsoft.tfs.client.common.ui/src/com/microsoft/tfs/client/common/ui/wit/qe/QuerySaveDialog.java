// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.wit.qe;

import java.io.File;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.framework.dialog.BaseDialog;
import com.microsoft.tfs.client.common.ui.wit.qe.QuerySaveControl.SaveMode;
import com.microsoft.tfs.core.clients.workitem.project.Project;
import com.microsoft.tfs.core.clients.workitem.project.ProjectCollection;
import com.microsoft.tfs.core.clients.workitem.queryhierarchy.QueryFolder;
import com.microsoft.tfs.util.GUID;

public class QuerySaveDialog extends BaseDialog {
    private final ProjectCollection projectCollection;
    private final String serverName;
    private final QueryFolder initialParent;
    private final SaveMode initialSaveMode;
    private final String initialQueryName;
    private final File initialSaveDirectory;

    private QuerySaveControl saveControl;

    public QuerySaveDialog(
        final Shell parentShell,
        final ProjectCollection projectCollection,
        final QueryFolder initialParent,
        final String serverName,
        final Project initialProject,
        final SaveMode initialSaveMode,
        final String initialQueryName,
        final File initialSaveDirectory) {
        super(parentShell);

        this.projectCollection = projectCollection;
        this.serverName = serverName;
        this.initialParent = initialParent;
        this.initialSaveMode = initialSaveMode;
        this.initialQueryName = initialQueryName;
        this.initialSaveDirectory = initialSaveDirectory;
    }

    @Override
    protected String provideDialogTitle() {
        return Messages.getString("QuerySaveDialog.SaveAsDialogTitle"); //$NON-NLS-1$
    }

    @Override
    protected void hookAddToDialogArea(final Composite dialogArea) {
        final FillLayout layout = new FillLayout();
        layout.marginWidth = getHorizontalMargin();
        layout.marginHeight = getVerticalMargin();
        dialogArea.setLayout(layout);

        saveControl = new QuerySaveControl(
            dialogArea,
            SWT.NONE,
            projectCollection,
            initialParent,
            serverName,
            initialSaveMode,
            initialQueryName,
            initialSaveDirectory);
    }

    @Override
    protected void buttonPressed(final int buttonId) {
        if (buttonId == IDialogConstants.OK_ID) {
            if (!saveControl.validate()) {
                return;
            }
        }

        super.buttonPressed(buttonId);
    }

    public File getSaveLocation() {
        return saveControl.getSaveLocation();
    }

    public String getQueryName() {
        return saveControl.getQueryName();
    }

    public GUID getParentGUID() {
        return saveControl.getParentGUID();
    }

    public Project getProject() {
        return saveControl.getProject();
    }

    public SaveMode getSaveMode() {
        return saveControl.getSaveMode();
    }

    public boolean getOverwriteExisting() {
        return saveControl.getOverwriteExisting();
    }
}
