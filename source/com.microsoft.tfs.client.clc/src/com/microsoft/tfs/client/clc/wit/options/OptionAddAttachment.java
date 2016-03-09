// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.clc.wit.options;

import java.io.File;
import java.text.MessageFormat;

import com.microsoft.tfs.client.clc.Messages;
import com.microsoft.tfs.client.clc.exceptions.InvalidOptionValueException;
import com.microsoft.tfs.client.clc.options.Option;

public class OptionAddAttachment extends Option {
    private String specifiedFilePath;
    private File localFile;
    private String comment;

    @Override
    public String getSyntaxString() {
        return "<local file path>[,comment]"; //$NON-NLS-1$
    }

    public String getComment() {
        return comment;
    }

    public File getLocalFile() {
        return localFile;
    }

    public String getSpecifiedFilePath() {
        return specifiedFilePath;
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

        specifiedFilePath = parts[0];
        localFile = new File(specifiedFilePath);

        if (parts.length > 1) {
            comment = parts[1];
        }
    }

    private void badOptionValue() throws InvalidOptionValueException {
        final String messageFormat = Messages.getString("OptionAddAttachment.OptionRequiresLocalPathCommentFormat"); //$NON-NLS-1$
        final String message = MessageFormat.format(messageFormat, getMatchedAlias());

        throw new InvalidOptionValueException(message);
    }
}
