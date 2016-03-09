// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.dialogs.vc;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.codemarker.CodeMarker;
import com.microsoft.tfs.client.common.codemarker.CodeMarkerDispatch;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.controls.vc.ReturnOnlineChangesTable;
import com.microsoft.tfs.client.common.ui.framework.dialog.BaseDialog;
import com.microsoft.tfs.client.common.ui.framework.sizing.ControlSize;
import com.microsoft.tfs.client.common.ui.framework.viewer.CheckboxEvent;
import com.microsoft.tfs.client.common.ui.framework.viewer.CheckboxListener;
import com.microsoft.tfs.client.common.ui.helpers.AutomationIDHelper;
import com.microsoft.tfs.core.clients.versioncontrol.offline.OfflineChange;

public class DetectLocalChangesDialog extends BaseDialog {
    public static final CodeMarker CODEMARKER_DETECT_LOCAL_CHANGES_FINISHED = new CodeMarker(
        "com.microsoft.tfs.client.common.ui.dialogs.vc.DetectLocalChangesDialog.#DetectLocalChangesDialogDisplayed"); //$NON-NLS-1$
    public static final String RETURNONLINECHANGES_TABLE_ID = "DetectLocalChangesDialog.changesTable"; //$NON-NLS-1$
    private OfflineChange[] changes;
    private ReturnOnlineChangesTable changesTable;

    public DetectLocalChangesDialog(final Shell parentShell, final OfflineChange[] changes) {
        super(parentShell);

        this.changes = changes;

        setOptionIncludeDefaultButtons(false);
        CodeMarkerDispatch.dispatch(CODEMARKER_DETECT_LOCAL_CHANGES_FINISHED);
    }

    @Override
    protected String provideDialogTitle() {
        return Messages.getString("DetectLocalChangesDialog.DialogTitle"); //$NON-NLS-1$
    }

    @Override
    protected void hookAddToDialogArea(final Composite dialogArea) {
        final GridLayout layout = new GridLayout(1, true);

        layout.horizontalSpacing = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);
        layout.verticalSpacing = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_SPACING);
        layout.marginWidth = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_MARGIN);
        layout.marginHeight = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_MARGIN);

        dialogArea.setLayout(layout);

        final Label explanationLabel = new Label(dialogArea, SWT.WRAP);
        explanationLabel.setText(Messages.getString("ReturnOnlineDialog.ExplainLabelText")); //$NON-NLS-1$
        explanationLabel.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, true, false));

        changesTable = new ReturnOnlineChangesTable(dialogArea, SWT.NONE);
        changesTable.setChanges(changes);
        changesTable.setCheckedChanges(changes);
        changesTable.addCheckboxListener(new CheckboxListener() {
            @Override
            public void checkedElementsChanged(final CheckboxEvent event) {
                getButton(IDialogConstants.OK_ID).setEnabled(changesTable.getCheckedElements().length > 0);
            }
        });
        AutomationIDHelper.setWidgetID(changesTable, RETURNONLINECHANGES_TABLE_ID);
        ControlSize.setCharHeightHint(changesTable, 10);
        ControlSize.setCharWidthHint(changesTable, 80);

        final GridData changesData = new GridData(SWT.FILL, SWT.FILL, true, true);
        changesData.grabExcessHorizontalSpace = true;
        changesData.grabExcessVerticalSpace = true;
        changesTable.setLayoutData(changesData);
    }

    /**
     * Override to set text on OK button. Simply setting the text,
     * unfortunately, does not size the button properly on OS X.
     */
    @Override
    protected void createButtonsForButtonBar(final Composite parent) {
        createButton(
            parent,
            IDialogConstants.OK_ID,
            Messages.getString("ReturnOnlineDialog.PendChangesButtonText"), //$NON-NLS-1$
            true);
        createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
    }

    @Override
    protected void hookDialogAboutToClose() {
        changes = changesTable.getCheckedChanges();
    }

    public OfflineChange[] getChanges() {
        return changes;
    }
}
