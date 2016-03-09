// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.clc.wit.options;

import java.text.MessageFormat;

import com.microsoft.tfs.client.clc.Messages;
import com.microsoft.tfs.client.clc.exceptions.InvalidOptionValueException;
import com.microsoft.tfs.client.clc.options.Option;

public class OptionAddRelated extends Option {
    private int workItemId;
    private String comment;

    @Override
    public String getSyntaxString() {
        return "<id>[,comment]"; //$NON-NLS-1$
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

        try {
            workItemId = Integer.parseInt(parts[0]);
        } catch (final NumberFormatException ex) {
            badOptionValue();
        }

        if (parts.length > 1) {
            comment = parts[1];
        }
    }

    public String getComment() {
        return comment;
    }

    public int getWorkItemID() {
        return workItemId;
    }

    private void badOptionValue() throws InvalidOptionValueException {
        final String messageFormat = Messages.getString("OptionAddRelated.OptionRequiresValidWorkItemIDCommentFormat"); //$NON-NLS-1$
        final String message = MessageFormat.format(messageFormat, getMatchedAlias());

        throw new InvalidOptionValueException(message);
    }
}
