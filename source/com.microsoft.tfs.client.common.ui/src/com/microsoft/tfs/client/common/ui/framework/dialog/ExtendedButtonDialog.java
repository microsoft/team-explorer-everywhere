// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.dialog;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

public abstract class ExtendedButtonDialog extends BaseDialog {
    /* Left-side button bar */
    private Control extendedButtonBar;

    /* Right-side (standard) button bar */
    private Control standardButtonBar;

    /*
     * An internal collection of button descriptions. When subclasses call
     * addButtonDescription(), the description is stored in the collection, and
     * when the dialog UI is being created, the description is used to create
     * the button widget.
     *
     * Note: a List is used to preseve the order that button descriptions are
     * added to the collection
     */
    private final List buttonDescriptions = new ArrayList();

    public ExtendedButtonDialog(final Shell parentShell) {
        super(parentShell);
    }

    /*
     * Override {@link
     * Dialog#createButtonBar(org.eclipse.swt.widgets.Composite)} so that we can
     * buttons to the left of the ok/cancel buttons, a la MSFT
     *
     * (non-Javadoc)
     *
     * @see
     * org.eclipse.jface.dialogs.Dialog#createButtonBar(org.eclipse.swt.widgets
     * .Composite)
     */
    @Override
    protected Control createButtonBar(final Composite parent) {
        final Composite fullButtonControl = new Composite(parent, SWT.NONE);

        fullButtonControl.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        fullButtonControl.setFont(parent.getFont());

        final GridLayout layout = new GridLayout();
        layout.numColumns = 2;
        layout.makeColumnsEqualWidth = false;
        layout.horizontalSpacing = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);
        layout.verticalSpacing = 0;
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        fullButtonControl.setLayout(layout);

        /* Set up an "extended" button bar on the left-hand side */
        extendedButtonBar = createExtendedButtonBar(fullButtonControl);
        extendedButtonBar.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        /* add the dialog's button bar to the right */
        standardButtonBar = super.createButtonBar(fullButtonControl);
        standardButtonBar.setLayoutData(new GridData(SWT.END, SWT.CENTER, true, false));

        return fullButtonControl;
    }

    /**
     * Adds a button description to this dialog. The button description is used
     * when the dialog's UI is being created to create a button widget in the
     * button bar.
     *
     * <p>
     * This method is for the benefit of subclasses. This base class does not
     * call it internally.
     *
     * <p>
     * If this method is called multiple times, the sequence is preserved, and
     * the buttons will be added in that sequence.
     *
     * <p>
     * If the default buttons are being included (see
     * setOptionIncludeDefaultButtons()), the buttons added because of calling
     * addButtonDescription are added <b>before</b> the default buttons.
     *
     * <p>
     * By calling setOptionIncludeDefaultButtons() and addButtonDescription()
     * subclasses can have complete control over the buttons in the button bar.
     *
     * @param buttonId
     * @param buttonLabel
     * @param isDefaultButton
     */
    protected void addExtendedButtonDescription(
        final int buttonId,
        final String buttonLabel,
        final boolean isDefaultButton) {
        buttonDescriptions.add(new ButtonDescription(buttonId, buttonLabel, isDefaultButton));
    }

    protected Control createExtendedButtonBar(final Composite parent) {
        final Composite composite = new Composite(parent, SWT.NONE);

        // create a layout with spacing and margins appropriate for the font
        // size.
        final GridLayout layout = new GridLayout();
        layout.numColumns = 0; // this is incremented by createButton
        layout.makeColumnsEqualWidth = true;
        layout.marginWidth = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_MARGIN);
        layout.marginHeight = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_MARGIN);
        layout.horizontalSpacing = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);
        layout.verticalSpacing = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_SPACING);
        composite.setLayout(layout);
        composite.setFont(parent.getFont());

        // Add the buttons to the button bar.
        createButtonsForExtendedButtonBar(composite);
        return composite;
    }

    /*
     * Subclasses may override this method to do their own button bar handling.
     * However, it is usually easier for subclasses to use the
     * setIncludeDefaultButtons() and addButtonDescription() methods instead.
     */
    protected void createButtonsForExtendedButtonBar(final Composite parent) {
        for (final Iterator it = buttonDescriptions.iterator(); it.hasNext();) {
            final ButtonDescription buttonDescription = (ButtonDescription) it.next();

            createExtendedButton(
                parent,
                buttonDescription.buttonId,
                buttonDescription.buttonLabel,
                buttonDescription.isDefault);
        }
    }

    protected Button createExtendedButton(
        final Composite parent,
        final int id,
        final String label,
        final boolean defaultButton) {
        return createButton(parent, id, label, defaultButton);
    }
}