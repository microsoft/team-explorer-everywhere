// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.clc.wit.options;

import java.text.MessageFormat;

import com.microsoft.tfs.client.clc.Messages;
import com.microsoft.tfs.client.clc.exceptions.InvalidOptionValueException;
import com.microsoft.tfs.client.clc.options.Option;

public class OptionAddExternalLink extends Option {
    private String linkType;
    private String uri;
    private String comment;

    @Override
    public String getSyntaxString() {
        return "<link type>,<artifact URI>[,comment]"; //$NON-NLS-1$
    }

    @Override
    public void parseValues(final String optionValueString) throws InvalidOptionValueException {
        if (optionValueString == null) {
            badOptionValue();
        }

        final String[] parts = optionValueString.split(","); //$NON-NLS-1$

        if (parts.length != 2 && parts.length != 3) {
            badOptionValue();
        }

        linkType = parts[0];
        uri = parts[1];

        if (parts.length > 2) {
            comment = parts[2];
        }
    }

    public String getComment() {
        return comment;
    }

    public String getLinkType() {
        return linkType;
    }

    public String getURI() {
        return uri;
    }

    private void badOptionValue() throws InvalidOptionValueException {
        final String messageFormat =
            Messages.getString("OptionAddExternalLink.OptionRequiresLinkTypeArtifactAndCommentFormat"); //$NON-NLS-1$
        final String message = MessageFormat.format(messageFormat, getMatchedAlias());

        throw new InvalidOptionValueException(message);
    }
}
