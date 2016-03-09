// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.form;

import java.text.MessageFormat;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.valid.AbstractValidator;
import com.microsoft.tfs.util.valid.IValidationMessage;
import com.microsoft.tfs.util.valid.IValidity;
import com.microsoft.tfs.util.valid.Severity;
import com.microsoft.tfs.util.valid.ValidationMessage;
import com.microsoft.tfs.util.valid.Validity;

public class TextFieldConditionallyRequiredValidator extends AbstractValidator {
    private final TextField.ValueChangedListener listener;
    private final TextField conditionField;
    private final Severity severity;

    public TextFieldConditionallyRequiredValidator(final TextField subject, final TextField conditionField) {
        this(subject, conditionField, Severity.ERROR);
    }

    public TextFieldConditionallyRequiredValidator(
        final TextField subject,
        final TextField conditionField,
        final Severity severity) {
        super(subject);

        Check.notNull(conditionField, "conditionField"); //$NON-NLS-1$
        this.conditionField = conditionField;

        if (severity != Severity.ERROR && severity != Severity.WARNING) {
            throw new IllegalArgumentException("illegal severity: " + severity); //$NON-NLS-1$
        }
        this.severity = severity;

        listener = new TextField.ValueChangedListener() {
            @Override
            public void onValueChanged(final String value, final TextField textField) {
                computeValidity();
            }
        };

        subject.addValueChangedListener(listener);
        conditionField.addValueChangedListener(listener);
        computeValidity();
    }

    @Override
    public void dispose() {
        final TextField subject = (TextField) getSubject();
        subject.removeValueChangedListener(listener);
        conditionField.removeValueChangedListener(listener);
    }

    private void computeValidity() {
        if (!conditionField.hasValue()) {
            setValid();
            return;
        }

        final TextField subject = (TextField) getSubject();

        if (subject.hasValue()) {
            setValid();
            return;
        }

        String message;
        if (severity == Severity.ERROR) {
            final String messageFormat =
                Messages.getString("TextFieldConditionallyRequiredValidator.MustEnterFieldValueFormat"); //$NON-NLS-1$
            message = MessageFormat.format(messageFormat, conditionField.getLabel(), subject.getLabel());
        } else {
            final String messageFormat =
                Messages.getString("TextFieldConditionallyRequiredValidator.ShouldEnterFieldValueFormat"); //$NON-NLS-1$
            message = MessageFormat.format(messageFormat, conditionField.getLabel(), subject.getLabel());
        }

        final IValidationMessage validationMessage = new ValidationMessage(message, severity);
        final IValidity validity = new Validity(validationMessage);
        setValidity(validity);
    }
}
