// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.clc.options;

import java.text.MessageFormat;

import com.microsoft.tfs.client.clc.Messages;
import com.microsoft.tfs.client.clc.OptionsMap;
import com.microsoft.tfs.client.clc.exceptions.InvalidOptionValueException;

public abstract class NoValueOption extends Option {
    public NoValueOption() {
        super();
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.microsoft.tfs.client.clc.options.Option#parseValues(java.lang.String)
     */
    @Override
    public void parseValues(final String optionValueString) throws InvalidOptionValueException {
        if (optionValueString != null) {
            final String messageFormat = Messages.getString("NoValueOption.NoValuesAcceptedFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, getMatchedAlias());
            throw new InvalidOptionValueException(message);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see com.microsoft.tfs.client.clc.options.Option#getSyntaxString()
     */
    @Override
    public String getSyntaxString() {
        return OptionsMap.getPreferredOptionPrefix() + getMatchedAlias();
    }
}
