// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.vc;

import java.text.MessageFormat;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.commands.vc.DeleteLabelCommand;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.framework.command.UICommandExecutorFactory;
import com.microsoft.tfs.client.common.ui.framework.dialog.ExtendedButtonDialog;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.tasks.vc.EditLabelTask;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.VersionControlLabel;

public class FindLabelDialog extends ExtendedButtonDialog {
    private static final int BUTTON_EDIT_ID = IDialogConstants.CLIENT_ID + 1;
    private static final int BUTTON_DELETE_ID = IDialogConstants.CLIENT_ID + 2;

    private final TFSRepository repository;
    private final String teamProjectServerPath;
    private FindLabelControl control;

    public FindLabelDialog(
        final Shell parentShell,
        final TFSRepository repository,
        final String teamProjectServerPath) {
        super(parentShell);
        this.repository = repository;
        this.teamProjectServerPath = teamProjectServerPath;

        setOptionIncludeDefaultButtons(false);

        addButtonDescription(IDialogConstants.OK_ID, IDialogConstants.CLOSE_LABEL, false);

        addExtendedButtonDescription(BUTTON_EDIT_ID, Messages.getString("FindLabelDialog.Edit"), false); //$NON-NLS-1$
        addExtendedButtonDescription(BUTTON_DELETE_ID, Messages.getString("FindLabelDialog.Delete"), false); //$NON-NLS-1$
    }

    public VersionControlLabel getSelectedLabel() {
        return control.getLabelsTable().getSelectedLabel();
    }

    @Override
    protected String provideDialogTitle() {
        return Messages.getString("FindLabelDialog.FindLabel"); //$NON-NLS-1$
    }

    @Override
    protected void hookAddToDialogArea(final Composite dialogArea) {
        final GridLayout layout = new GridLayout();
        layout.marginWidth = getHorizontalMargin();
        layout.marginHeight = getVerticalMargin();
        layout.horizontalSpacing = getHorizontalSpacing();
        layout.verticalSpacing = getVerticalSpacing();
        dialogArea.setLayout(layout);

        control = new FindLabelControl(dialogArea, SWT.NONE, repository, teamProjectServerPath, "FindLabelDialog"); //$NON-NLS-1$
        control.getLabelsTable().addDoubleClickListener(new IDoubleClickListener() {
            @Override
            public void doubleClick(final DoubleClickEvent event) {
                final VersionControlLabel selection = control.getLabelsTable().getSelectedLabel();

                if (selection == null) {
                    return;
                }

                editLabel(selection);
            }
        });
        control.getLabelsTable().addSelectionChangedListener(new ISelectionChangedListener() {
            @Override
            public void selectionChanged(final SelectionChangedEvent event) {
                getButton(BUTTON_EDIT_ID).setEnabled(control.getLabelsTable().getSelectedLabels().length == 1);
                getButton(BUTTON_DELETE_ID).setEnabled(control.getLabelsTable().getSelectedLabels().length == 1);
            }
        });
        GridDataBuilder.newInstance().grab().fill().wHint(getMinimumMessageAreaWidth()).applyTo(control);
    }

    @Override
    protected void createButtonsForButtonBar(final Composite parent) {
        super.createButtonsForButtonBar(parent);
    }

    @Override
    protected void hookAfterButtonsCreated() {
        getButton(BUTTON_EDIT_ID).setEnabled(false);
        getButton(BUTTON_DELETE_ID).setEnabled(false);
    }

    @Override
    protected void hookCustomButtonPressed(final int buttonId) {
        if (buttonId == BUTTON_EDIT_ID) {
            final VersionControlLabel editLabel = control.getLabelsTable().getSelectedLabel();

            if (editLabel == null) {
                return;
            }

            editLabel(editLabel);
        } else if (buttonId == BUTTON_DELETE_ID) {
            final VersionControlLabel deleteLabel = control.getLabelsTable().getSelectedLabel();

            if (deleteLabel == null) {
                return;
            }

            deleteLabel(deleteLabel);
        }
    }

    private void editLabel(final VersionControlLabel label) {
        final EditLabelTask editTask = new EditLabelTask(getShell(), repository, label);
        final IStatus editStatus = editTask.run();

        if (!editStatus.isOK()) {
            return;
        }

        final VersionControlLabel newLabel = editTask.getLabel();

        if (editTask.isDeleted()) {
            control.deleteLabel(label);
        } else {
            control.updateLabel(label, newLabel);
        }
    }

    private void deleteLabel(final VersionControlLabel label) {
        if (!MessageDialog.openQuestion(
            getShell(),
            MessageFormat.format(Messages.getString("FindLabelDialog.DeleteLabelFormat"), label.getName()), //$NON-NLS-1$
            Messages.getString("FindLabelDialog.AreYouSureYouWishToDeleteThisLabel"))) //$NON-NLS-1$
        {
            return;
        }

        final DeleteLabelCommand deleteCommand = new DeleteLabelCommand(repository, label);
        final IStatus deleteStatus = UICommandExecutorFactory.newUICommandExecutor(getShell()).execute(deleteCommand);

        if (!deleteStatus.isOK()) {
            return;
        }

        control.deleteLabel(label);
    }
}
