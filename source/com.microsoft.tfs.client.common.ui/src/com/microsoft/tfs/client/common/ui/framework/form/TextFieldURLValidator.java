// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.form;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.util.valid.IValidity;
import com.microsoft.tfs.util.valid.Validity;

public class TextFieldURLValidator extends TextFieldRequiredValidator {
    public TextFieldURLValidator(final TextField subject) {
        super(subject);
    }

    @Override
    protected IValidity computeValidity(final TextField textField) {
        final IValidity validity = super.computeValidity(textField);
        if (!validity.isValid()) {
            return validity;
        }

        final String value = textField.getValue();

        try {
            new URL(value);
            return validity;
        } catch (final MalformedURLException e) {
            final String messageFormat = Messages.getString("TextFieldUrlValidator.MustEnterUrlForFieldFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, textField.getLabel());
            return Validity.invalid(message);
        }
    }
}
