// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.connect;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;

import org.eclipse.swt.widgets.Text;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.framework.validation.AbstractTextControlValidator;
import com.microsoft.tfs.util.valid.IValidity;
import com.microsoft.tfs.util.valid.Validity;

public class TextControlURLValidator extends AbstractTextControlValidator {
    private String fieldName;
    private final boolean required;

    private URL url;

    public TextControlURLValidator(final Text subject, final String fieldName, final boolean required) {
        super(subject);
        this.fieldName = fieldName;
        this.required = required;
        validate();
    }

    public void setFieldName(final String fieldName) {
        this.fieldName = fieldName;
    }

    public URL getURL() {
        return url;
    }

    @Override
    protected IValidity computeValidity(String text) {
        text = text.trim();

        if (text.length() == 0) {
            url = null;
            if (required) {
                return Validity.invalid(
                    MessageFormat.format(
                        Messages.getString("TextControlUrlValidator.MustEnterValueFormat"), //$NON-NLS-1$
                        fieldName));
            }
            return Validity.VALID;
        }

        try {
            url = new URL(text);
        } catch (final MalformedURLException e) {
            url = null;
            final String message =
                MessageFormat.format(Messages.getString("TextControlUrlValidator.MustEnterValidUrlFormat"), fieldName); //$NON-NLS-1$
            return Validity.invalid(message);
        }

        if (url.getHost().indexOf('_') != -1) {
            return Validity.invalid(Messages.getString("TextControlUrlValidator.UnderscoreNotAllowedInHostname")); //$NON-NLS-1$
        }

        return Validity.VALID;
    }
}
