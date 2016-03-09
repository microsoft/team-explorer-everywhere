// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teambuild.dialogs;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.ui.framework.dialog.BaseDialog;
import com.microsoft.tfs.client.common.ui.framework.helper.SWTUtil;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.teambuild.Messages;
import com.microsoft.tfs.core.clients.build.flags.DeleteOptions;

public abstract class DeleteOptionsDialog extends BaseDialog {

    private Button detailsCheckbox;
    private Button dropCheckbox;
    private Button testResultsCheckbox;
    private Button labelCheckbox;
    private Button symbolsCheckbox;
    private final boolean isV3OrGreater;
    private DeleteOptions deleteOption = DeleteOptions.ALL;

    public DeleteOptionsDialog(final Shell parentShell) {
        super(parentShell);
        this.isV3OrGreater = true;
    }

    public DeleteOptionsDialog(final Shell parentShell, final boolean isV3OrGreater) {
        super(parentShell);
        this.deleteOption = DeleteOptions.ALL;
        this.isV3OrGreater = isV3OrGreater;
    }

    public DeleteOptionsDialog(final Shell parentShell, final DeleteOptions deleteOption) {
        super(parentShell);
        this.isV3OrGreater = true;
        this.deleteOption = deleteOption;
    }

    @Override
    protected void hookAddToDialogArea(final Composite dialogArea) {
        this.getShell().setMinimumSize(600, 200);
        final GridLayout layout = SWTUtil.gridLayout(dialogArea);
        layout.marginWidth = getHorizontalMargin();
        layout.marginHeight = getVerticalMargin();
        layout.verticalSpacing = 10;

        Label label = new Label(dialogArea, SWT.NONE);
        label.setText(getDescriptionText());
        GridDataBuilder.newInstance().applyTo(label);

        final FontData[] fontData = label.getFont().getFontData();
        fontData[0].setStyle(SWT.BOLD);
        final Font boldFont = new Font(label.getFont().getDevice(), fontData[0]);
        detailsCheckbox = new Button(dialogArea, SWT.CHECK);
        detailsCheckbox.setText(detailsButtonLabel());
        detailsCheckbox.setFont(boldFont);
        GridDataBuilder.newInstance().applyTo(detailsCheckbox);

        final GridDataBuilder labelGridDataBuilder = GridDataBuilder.newInstance().hIndent(24).hFill().hGrab().wHint(
            IDialogConstants.MINIMUM_MESSAGE_AREA_WIDTH);
        label = new Label(dialogArea, SWT.WRAP);
        label.setText(Messages.getString("DeleteOptionsDialog.DetailsButtonTooltipText")); //$NON-NLS-1$
        labelGridDataBuilder.applyTo(label);

        if (isDetailsDefault()) {
            detailsCheckbox.setEnabled(false);
            label.setEnabled(false);
        }

        dropCheckbox = new Button(dialogArea, SWT.CHECK);
        dropCheckbox.setText(Messages.getString("DeleteOptionsDialog.DropButtonLabel")); //$NON-NLS-1$
        dropCheckbox.setFont(boldFont);
        GridDataBuilder.newInstance().applyTo(dropCheckbox);

        label = new Label(dialogArea, SWT.WRAP);
        label.setText(Messages.getString("DeleteOptionsDialog.DropButtonTooltipText")); //$NON-NLS-1$
        labelGridDataBuilder.applyTo(label);

        testResultsCheckbox = new Button(dialogArea, SWT.CHECK);
        testResultsCheckbox.setText(Messages.getString("DeleteOptionsDialog.TestResultsButtonLabel")); //$NON-NLS-1$
        testResultsCheckbox.setFont(boldFont);
        GridDataBuilder.newInstance().applyTo(detailsCheckbox);

        label = new Label(dialogArea, SWT.WRAP);
        label.setText(Messages.getString("DeleteOptionsDialog.TestResultsButtonTooltipText")); //$NON-NLS-1$
        labelGridDataBuilder.applyTo(label);

        labelCheckbox = new Button(dialogArea, SWT.CHECK);
        labelCheckbox.setText(Messages.getString("DeleteOptionsDialog.LabelButtonLabel")); //$NON-NLS-1$
        labelCheckbox.setFont(boldFont);
        GridDataBuilder.newInstance().applyTo(detailsCheckbox);

        label = new Label(dialogArea, SWT.WRAP);
        label.setText(Messages.getString("DeleteOptionsDialog.LabelButtonTooltipText")); //$NON-NLS-1$
        labelGridDataBuilder.applyTo(label);

        symbolsCheckbox = new Button(dialogArea, SWT.CHECK);
        symbolsCheckbox.setText(Messages.getString("DeleteOptionsDialog.SymbolsButtonLabel")); //$NON-NLS-1$
        symbolsCheckbox.setFont(boldFont);
        GridDataBuilder.newInstance().applyTo(detailsCheckbox);

        label = new Label(dialogArea, SWT.WRAP);
        label.setText(Messages.getString("DeleteOptionsDialog.SymbolsButtonTooltipText")); //$NON-NLS-1$
        labelGridDataBuilder.applyTo(label);

        attachSelectionListener(detailsCheckbox);
        attachSelectionListener(dropCheckbox);
        attachSelectionListener(testResultsCheckbox);
        attachSelectionListener(labelCheckbox);
        attachSelectionListener(symbolsCheckbox);

        dialogArea.addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(final DisposeEvent arg0) {
                boldFont.dispose();
            }
        });

        if (!isV3OrGreater) {
            detailsCheckbox.setSelection(true);
            dropCheckbox.setSelection(true);
            testResultsCheckbox.setSelection(true);
            labelCheckbox.setSelection(true);
            symbolsCheckbox.setSelection(true);
            disableComposite(dialogArea);
        } else {
            setSelections();
        }
    }

    private void setSelections() {
        if (deleteOption.contains(DeleteOptions.DETAILS)) {
            detailsCheckbox.setSelection(true);
        }
        if (deleteOption.contains(DeleteOptions.DROP_LOCATION)) {
            dropCheckbox.setSelection(true);
        }
        if (deleteOption.contains(DeleteOptions.TEST_RESULTS)) {
            testResultsCheckbox.setSelection(true);
        }
        if (deleteOption.contains(DeleteOptions.LABEL)) {
            labelCheckbox.setSelection(true);
        }
        if (deleteOption.contains(DeleteOptions.SYMBOLS)) {
            symbolsCheckbox.setSelection(true);
        }
    }

    private void disableComposite(final Composite composite) {
        final Control[] children = composite.getChildren();

        for (final Control control : children) {
            if (control instanceof Label) {
                if (((Label) control).getText().equals(getDescriptionText())) {
                    continue;
                }
            }
            control.setEnabled(false);
        }
    }

    private void attachSelectionListener(final Button button) {
        button.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                validate();
            }
        });
    }

    private void validate() {
        DeleteOptions option = DeleteOptions.NONE;
        if (detailsCheckbox.getSelection()) {
            option = option.combine(DeleteOptions.DETAILS);
        }
        if (dropCheckbox.getSelection()) {
            option = option.combine(DeleteOptions.DROP_LOCATION);
        }
        if (testResultsCheckbox.getSelection()) {
            option = option.combine(DeleteOptions.TEST_RESULTS);
        }
        if (labelCheckbox.getSelection()) {
            option = option.combine(DeleteOptions.LABEL);
        }
        if (symbolsCheckbox.getSelection()) {
            option = option.combine(DeleteOptions.SYMBOLS);
        }
        if (option.equals(DeleteOptions.NONE)) {
            getButton(IDialogConstants.OK_ID).setEnabled(false);
        } else {
            getButton(IDialogConstants.OK_ID).setEnabled(true);
        }
        deleteOption = option;
    }

    public DeleteOptions getDeleteOption() {
        return deleteOption;
    }

    protected abstract String detailsButtonLabel();

    protected abstract String getDescriptionText();

    protected abstract boolean isDetailsDefault();
}
