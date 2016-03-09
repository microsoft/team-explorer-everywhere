// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.form;

import java.text.MessageFormat;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.util.valid.AbstractValidator;
import com.microsoft.tfs.util.valid.IValidationMessage;
import com.microsoft.tfs.util.valid.IValidity;
import com.microsoft.tfs.util.valid.Severity;
import com.microsoft.tfs.util.valid.ValidationMessage;
import com.microsoft.tfs.util.valid.Validity;

public class TextFieldRequiredValidator extends AbstractValidator {
    private final TextField.ValueChangedListener listener;
    private final Severity severity;

    public TextFieldRequiredValidator(final TextField subject) {
        this(subject, Severity.ERROR);
    }

    public TextFieldRequiredValidator(final TextField subject, final Severity severity) {
        super(subject);

        if (severity != Severity.WARNING && severity != Severity.ERROR) {
            throw new IllegalArgumentException("illegal severity: " + severity); //$NON-NLS-1$
        }

        this.severity = severity;

        listener = new TextField.ValueChangedListener() {
            @Override
            public void onValueChanged(final String value, final TextField textField) {
                updateValidity();
            }
        };

        subject.addValueChangedListener(listener);
        updateValidity();
    }

    @Override
    public void dispose() {
        final TextField subject = (TextField) getSubject();
        subject.removeValueChangedListener(listener);
    }

    private void updateValidity() {
        final TextField subject = (TextField) getSubject();
        final IValidity validity = computeValidity(subject);
        setValidity(validity);
    }

    protected IValidity computeValidity(final TextField textField) {
        if (textField.hasValue()) {
            return Validity.VALID;
        } else {
            String message = null;
            if (severity == Severity.WARNING) {
                final String messageFormat = Messages.getString("TextFieldRequiredValidator.MissingValueWarningFormat"); //$NON-NLS-1$
                message = MessageFormat.format(messageFormat, textField.getLabel());
            } else {
                final String messageFormat = Messages.getString("TextFieldRequiredValidator.MissingValueErrorFormat"); //$NON-NLS-1$
                message = MessageFormat.format(messageFormat, textField.getLabel());
            }

            final IValidationMessage validationMessage = new ValidationMessage(message, severity);
            return new Validity(validationMessage);
        }
    }
}
