// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.dialogs.vc;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.controls.generic.compatibility.table.DoubleClickEvent;
import com.microsoft.tfs.client.common.ui.controls.generic.compatibility.table.DoubleClickListener;
import com.microsoft.tfs.client.common.ui.controls.vc.FindChangesetControl;
import com.microsoft.tfs.client.common.ui.framework.dialog.ExtendedButtonDialog;
import com.microsoft.tfs.client.common.ui.framework.validation.ButtonValidatorBinding;
import com.microsoft.tfs.client.common.ui.framework.validation.NumericConstraint;
import com.microsoft.tfs.client.common.ui.framework.validation.SelectionProviderValidator;
import com.microsoft.tfs.client.common.ui.tasks.vc.ViewChangesetDetailsTask;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Changeset;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.valid.Validator;

public class FindChangesetDialog extends ExtendedButtonDialog {
    private static final int DETAILS_ID = IDialogConstants.CLIENT_ID + 1;

    private final TFSRepository repository;
    private String path;
    private FindChangesetControl control;

    /*
     * closeOnlyMode is specified when the dialog should only have a close
     * button (not OK/Cancel). This is used by GotoChangesetDialog, or other
     * places where we merely want to display, not allow the user to select, a
     * changeset.
     */
    private boolean closeOnlyMode = false;

    public FindChangesetDialog(final Shell parentShell, final TFSRepository repository) {
        super(parentShell);

        Check.notNull(repository, "repository"); //$NON-NLS-1$
        this.repository = repository;

        setOptionIncludeDefaultButtons(false);
        addExtendedButtonDescription(DETAILS_ID, Messages.getString("FindChangesetDialog.DetailsButtonText"), false); //$NON-NLS-1$
    }

    public void setCloseOnlyMode(final boolean closeOnlyMode) {
        this.closeOnlyMode = closeOnlyMode;
    }

    public Changeset getSelectedChangeset() {
        return control.getHistoryControl().getSelectedChangeset();
    }

    public void setPath(final String path) {
        this.path = path;

        if (control != null) {
            control.getOptionsControl().setPath(path);
        }
    }

    @Override
    protected void hookCustomButtonPressed(final int buttonId) {
        if (DETAILS_ID == buttonId) {
            showSelectedChangesetDetails();
        }
    }

    @Override
    protected String provideDialogTitle() {
        return Messages.getString("FindChangesetDialog.FindChangesetDialogTitle"); //$NON-NLS-1$
    }

    @Override
    protected void hookAddToDialogArea(final Composite dialogArea) {
        final FillLayout layout = new FillLayout();
        layout.marginWidth = getHorizontalMargin();
        layout.marginHeight = getVerticalMargin();
        dialogArea.setLayout(layout);

        control = new FindChangesetControl(dialogArea, SWT.NONE, repository);
        if (path != null) {
            control.getOptionsControl().setPath(path);
        }

        control.getHistoryControl().addDoubleClickListener(new DoubleClickListener() {
            @Override
            public void doubleClick(final DoubleClickEvent event) {
                if (closeOnlyMode) {
                    showSelectedChangesetDetails();
                } else {
                    okPressed();
                }
            }
        });

        control.setFocus();
    }

    @Override
    protected void createButtonsForButtonBar(final Composite parent) {
        if (closeOnlyMode) {
            addButtonDescription(IDialogConstants.CANCEL_ID, IDialogConstants.CLOSE_LABEL, true);
        } else {
            addButtonDescription(IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
            addButtonDescription(IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
        }

        super.createButtonsForButtonBar(parent);
    }

    @Override
    protected void hookAfterButtonsCreated() {
        final Validator validator =
            new SelectionProviderValidator(control.getHistoryControl(), NumericConstraint.EXACTLY_ONE, null);

        new ButtonValidatorBinding(getButton(DETAILS_ID)).bind(validator);

        if (!closeOnlyMode) {
            new ButtonValidatorBinding(getButton(IDialogConstants.OK_ID)).bind(validator);
        }
    }

    private void showSelectedChangesetDetails() {
        final Changeset selectedChangeset = control.getHistoryControl().getSelectedChangeset();

        if (selectedChangeset != null) {
            new ViewChangesetDetailsTask(getShell(), repository, selectedChangeset.getChangesetID()).run();
        }
    }
}
