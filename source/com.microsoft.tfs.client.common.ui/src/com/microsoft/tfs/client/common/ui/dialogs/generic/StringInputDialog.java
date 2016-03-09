// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.dialogs.generic;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.microsoft.tfs.client.common.ui.framework.dialog.BaseDialog;
import com.microsoft.tfs.client.common.ui.framework.helper.FormHelper;
import com.microsoft.tfs.client.common.ui.framework.sizing.ControlSize;
import com.microsoft.tfs.client.common.ui.helpers.AutomationIDHelper;

/**
 * A simple Dialog that prompts the user to input a String.
 *
 * The String is required by default, so to change to non-required behavior call
 * setRequired(false).
 */
public class StringInputDialog extends BaseDialog {
    private final String label;
    private String text;
    private final String dialogTitle;
    private boolean required = true;
    private Text inputText;
    private int selectionStart = -1;
    private int selectionEnd = -1;

    public static final String INPUT_TEXT_ID = "StringInputDialog.inputText"; //$NON-NLS-1$

    /**
     * Create a new Dialog.
     *
     * @param parentShell
     *        the Shell to use
     * @param label
     *        the label that appears by the Text input box
     * @param initialValue
     *        the initial value to populate the input box with, or null
     * @param dialogTitle
     *        the title of the Dialog
     * @param purpose
     *        a key for this dialog usage, used for persistent settings, never
     *        seen by the user
     */
    public StringInputDialog(
        final Shell parentShell,
        final String label,
        final String initialValue,
        final String dialogTitle,
        final String purpose) {
        super(parentShell);
        this.label = label;
        text = initialValue;
        this.dialogTitle = dialogTitle;

        /*
         * Set the dialog settings key to be "purpose" This allows different
         * uses of the generic DlgInputString to have different settings
         */
        setOptionDialogSettingsKey(StringInputDialog.class.getName() + "." + purpose); //$NON-NLS-1$
    }

    public void setRequired(final boolean required) {
        this.required = required;
    }

    @Override
    protected Control createDialogArea(final Composite parent) {
        final Composite container = (Composite) super.createDialogArea(parent);

        final FormLayout formLayout = new FormLayout();
        formLayout.spacing = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);
        formLayout.marginWidth = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_MARGIN);
        formLayout.marginHeight = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_MARGIN);
        container.setLayout(formLayout);

        final Label inputLabel = new Label(container, SWT.NONE);

        inputText = new Text(container, SWT.BORDER);
        AutomationIDHelper.setWidgetID(inputText, INPUT_TEXT_ID);

        inputLabel.setText(label);
        final FormData fd1 = new FormData();
        fd1.left = new FormAttachment(0, 0);
        fd1.top = new FormAttachment(0, FormHelper.VerticalOffset(inputLabel, inputText));
        inputLabel.setLayoutData(fd1);

        if (text != null) {
            inputText.setText(text);
        }

        if (text != null && selectionStart >= 0 && selectionEnd >= 0) {
            inputText.setSelection(selectionStart, selectionEnd);
        } else if (text != null) {
            inputText.setSelection(text.length());
        }

        final FormData fd2 = new FormData();
        fd2.left = new FormAttachment(inputLabel, 0, SWT.RIGHT);
        fd2.top = new FormAttachment(0, 0);
        fd2.right = new FormAttachment(100, 0);
        inputText.setLayoutData(fd2);

        inputText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(final ModifyEvent e) {
                processInput();
            }
        });

        ControlSize.setCharWidthHint(inputText, 60);

        return container;
    }

    public void setSelection(final int start) {
        this.selectionStart = start;
        this.selectionEnd = start;
    }

    public void setSelection(final int start, final int end) {
        this.selectionStart = start;
        this.selectionEnd = end;
    }

    public String getInput() {
        return text;
    }

    private void processInput() {
        final Button okButton = getButton(IDialogConstants.OK_ID);

        /*
         * sanity check
         */
        if (okButton == null) {
            return;
        }

        text = inputText.getText() != null ? inputText.getText().trim() : ""; //$NON-NLS-1$

        okButton.setEnabled(required ? (text.length() > 0) : true);
    }

    @Override
    protected void hookAfterButtonsCreated() {
        processInput();
    }

    @Override
    protected String provideDialogTitle() {
        return dialogTitle;
    }
}