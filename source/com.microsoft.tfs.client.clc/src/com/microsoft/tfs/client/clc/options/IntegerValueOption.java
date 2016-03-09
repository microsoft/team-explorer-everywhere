// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.clc.options;

import java.text.MessageFormat;

import com.microsoft.tfs.client.clc.Messages;
import com.microsoft.tfs.client.clc.exceptions.InvalidOptionValueException;

public abstract class IntegerValueOption extends SingleValueOption {
    private int _value;

    public IntegerValueOption() {
        super();
    }

    @Override
    protected final String[] getValidOptionValues() {
        /*
         * null means that all values are permitted for this option.
         */
        return null;
    }

    @Override
    public void parseValues(final String optionValueString) throws InvalidOptionValueException {
        if (optionValueString == null) {
            throwIntegerValueStyleException();
        }

        /*
         * Let the superclass parse the string into the value.
         */
        super.parseValues(optionValueString);

        try {
            _value = Integer.parseInt(getValue());
        } catch (final NumberFormatException e) {
            throwIntegerValueStyleException();
        }
    }

    private void throwIntegerValueStyleException() throws InvalidOptionValueException {
        final String messageFormat = Messages.getString("IntegerValueOption.OptionRequiresAnIntegerFormat"); //$NON-NLS-1$
        final String message = MessageFormat.format(messageFormat, getMatchedAlias());
        throw new InvalidOptionValueException(message);
    }

    public int getNumber() {
        return _value;
    }
}
