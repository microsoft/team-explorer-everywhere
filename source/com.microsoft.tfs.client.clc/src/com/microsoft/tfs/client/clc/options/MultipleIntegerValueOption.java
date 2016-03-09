// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.clc.options;

import java.text.MessageFormat;

import com.microsoft.tfs.client.clc.Messages;
import com.microsoft.tfs.client.clc.OptionsMap;
import com.microsoft.tfs.client.clc.exceptions.InvalidOptionValueException;

public abstract class MultipleIntegerValueOption extends MultipleValueOption {
    private int[] numbers = new int[0];

    @Override
    public void parseValues(final String optionValueString) throws InvalidOptionValueException {
        super.parseValues(optionValueString);

        numbers = new int[getValues().length];

        for (int i = 0; i < getValues().length; i++) {
            try {
                numbers[i] = Integer.parseInt(getValues()[i]);
            } catch (final NumberFormatException e) {
                final String messageFormat = Messages.getString("MultipleIntegerValueOption.NotAValidIntegerFormat"); //$NON-NLS-1$
                final String message = MessageFormat.format(messageFormat, getValues()[i]);
                throw new InvalidOptionValueException(message);
            }
        }
    }

    @Override
    protected String[] getValidOptionValues() {
        return null;
    }

    public int[] getIntegerValues() {
        return numbers;
    }

    @Override
    public String getSyntaxString() {
        return OptionsMap.getPreferredOptionPrefix() + getMatchedAlias() + ":" + "<integer>[,<integer>...]"; //$NON-NLS-1$ //$NON-NLS-2$
    }
}
