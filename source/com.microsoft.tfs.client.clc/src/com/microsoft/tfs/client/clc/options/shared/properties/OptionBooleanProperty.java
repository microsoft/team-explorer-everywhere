// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.clc.options.shared.properties;

import java.text.MessageFormat;

import com.microsoft.tfs.client.clc.Messages;
import com.microsoft.tfs.client.clc.exceptions.InvalidOptionValueException;
import com.microsoft.tfs.util.Check;

public class OptionBooleanProperty extends OptionPropertyBase {
    public final static String TRUE_VALUE = "true"; //$NON-NLS-1$
    public final static String FALSE_VALUE = "false"; //$NON-NLS-1$

    private boolean value;

    public OptionBooleanProperty() {
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

        /* Don't parse a prompt value */
        if (isPrompt()) {
            return;
        }

        final String propertyValueString = getPropertyValue();

        setBoolean(propertyValueString);
    }

    @Override
    public void setPropertyValue(final String propertyValue) throws InvalidOptionValueException {
        Check.notNull(propertyValue, "propertyValue"); //$NON-NLS-1$

        super.setPropertyValue(propertyValue);
        setBoolean(propertyValue);
    }

    private void throwBooleanValueStyleException() throws InvalidOptionValueException {

        final String messageFormat = Messages.getString("OptionBooleanProperty.OptionRequiresThisOrThatAsValuesFormat"); //$NON-NLS-1$
        final String message = MessageFormat.format(messageFormat, getMatchedAlias(), TRUE_VALUE, FALSE_VALUE);
        throw new InvalidOptionValueException(message);
    }

    private void setBoolean(final String stringValue) throws InvalidOptionValueException {
        Check.notNull(stringValue, "stringValue"); //$NON-NLS-1$

        if (stringValue.equalsIgnoreCase(TRUE_VALUE) == false && stringValue.equalsIgnoreCase(FALSE_VALUE) == false) {
            throwBooleanValueStyleException();
        }

        value = Boolean.valueOf(stringValue).booleanValue();
    }

    public boolean getBoolean() {
        return value;
    }

    @Override
    protected String getPropertyValueSyntaxPart() {
        return TRUE_VALUE + "|" + FALSE_VALUE; //$NON-NLS-1$
    }
}
