// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.connect;

import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.widgets.Text;

import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.valid.AbstractValidator;
import com.microsoft.tfs.util.valid.Validity;

public class TextControlRequiredWhenOtherTextSpecifiedValidator extends AbstractValidator {
    private final ModifyListener modifyListener;

    private final Text subject;
    private final Text otherTextControl;
    private final String errorMessage;

    protected TextControlRequiredWhenOtherTextSpecifiedValidator(
        final Text subject,
        final Text otherTextControl,
        final String errorMessage) {
        super(subject);

        Check.notNull(subject, "subject"); //$NON-NLS-1$
        Check.notNull(otherTextControl, "otherTextControl"); //$NON-NLS-1$
        Check.notNull(errorMessage, "errorMessage"); //$NON-NLS-1$

        this.subject = subject;
        this.otherTextControl = otherTextControl;
        this.errorMessage = errorMessage;

        modifyListener = new ModifyListener() {
            @Override
            public void modifyText(final ModifyEvent e) {
                validate();
            }
        };

        subject.addModifyListener(modifyListener);
        otherTextControl.addModifyListener(modifyListener);
    }

    @Override
    public void dispose() {
        subject.removeModifyListener(modifyListener);
        otherTextControl.removeModifyListener(modifyListener);
    }

    private void validate() {
        final String otherText = otherTextControl.getText().trim();

        /* If there's nothing in the dependent text field, everything is ok. */
        if (otherText.length() == 0) {
            setValidity(Validity.VALID);
            return;
        }

        /* Otherwise, ensure the subject text controls has a value also */
        final String subjectText = subject.getText().trim();

        if (subjectText.length() == 0) {
            setValidity(Validity.invalid(errorMessage));
            return;
        }

        setValidity(Validity.VALID);
    }
}
