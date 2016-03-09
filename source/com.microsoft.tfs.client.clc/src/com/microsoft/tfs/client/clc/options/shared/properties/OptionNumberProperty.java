// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.clc.options.shared.properties;

import java.text.MessageFormat;

import com.microsoft.tfs.client.clc.Messages;
import com.microsoft.tfs.client.clc.exceptions.InvalidOptionValueException;
import com.microsoft.tfs.util.Check;

public class OptionNumberProperty extends OptionPropertyBase {
    private long value;

    public OptionNumberProperty() {
        super();
    }

    @Override
    public void parseValues(final String optionValueString) throws InvalidOptionValueException {
        /*
         * Let the superclass parse the string into the property name and value
         * fields.
         */
        super.parseValues(optionValueString);

        /*
         * Don't parse for a reset option.
         */
        if (isDelete()) {
            return;
        }

        /* Don't parse for a prompt option. */
        if (isPrompt()) {
            return;
        }

        final String propertyValueString = getPropertyValue();

        setNumber(propertyValueString);
    }

    @Override
    public void setPropertyValue(final String propertyValue) throws InvalidOptionValueException {
        Check.notNull(propertyValue, "propertyValue"); //$NON-NLS-1$

        super.setPropertyValue(propertyValue);
        setNumber(propertyValue);
    }

    private void setNumber(final String stringValue) throws InvalidOptionValueException {
        try {
            value = Long.parseLong(stringValue);
        } catch (final NumberFormatException e) {
            throwLongValueStyleException();
        }
    }

    private void throwLongValueStyleException() throws InvalidOptionValueException {
        final String messageFormat = Messages.getString("OptionNumberProperty.OptionRequiresAnIntegerFormat"); //$NON-NLS-1$
        final String message = MessageFormat.format(messageFormat, getMatchedAlias());
        throw new InvalidOptionValueException(message);
    }

    @Override
    protected String getPropertyValueSyntaxPart() {
        return "<number>"; //$NON-NLS-1$
    }

    public long getNumber() {
        return value;
    }
}
