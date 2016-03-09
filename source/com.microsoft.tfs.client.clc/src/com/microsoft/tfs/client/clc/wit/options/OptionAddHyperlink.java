// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.clc.wit.options;

import java.text.MessageFormat;

import com.microsoft.tfs.client.clc.Messages;
import com.microsoft.tfs.client.clc.exceptions.InvalidOptionValueException;
import com.microsoft.tfs.client.clc.options.Option;

public class OptionAddHyperlink extends Option {
    private String location;
    private String comment;

    @Override
    public String getSyntaxString() {
        return "<location>[,comment]"; //$NON-NLS-1$
    }

    @Override
    public void parseValues(final String optionValueString) throws InvalidOptionValueException {
        if (optionValueString == null) {
            badOptionValue();
        }

        final String[] parts = optionValueString.split(","); //$NON-NLS-1$

        if (parts.length != 1 && parts.length != 2) {
            badOptionValue();
        }

        location = parts[0];

        if (parts.length > 1) {
            comment = parts[1];
        }
    }

    public String getComment() {
        return comment;
    }

    public String getLocation() {
        return location;
    }

    private void badOptionValue() throws InvalidOptionValueException {
        final String messageFormat = Messages.getString("OptionAddHyperlink.OptionRequiresHyperlinkCommentFormat"); //$NON-NLS-1$
        final String message = MessageFormat.format(messageFormat, getMatchedAlias());

        throw new InvalidOptionValueException(message);
    }
}
