// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.clc.exceptions;

import java.text.MessageFormat;

import com.microsoft.tfs.client.clc.Messages;

/**
 *         Thrown when a command is parsed, but the name does not match any
 *         known command.
 */
public final class UnknownCommandException extends ArgumentException {
    static final long serialVersionUID = -3529197730189982512L;

    public UnknownCommandException() {
        super();
    }

    public UnknownCommandException(final String userText) {
        super(makeText(userText));
    }

    private static String makeText(final String userText) {
        if (userText != null && userText.startsWith("@")) //$NON-NLS-1$
        {
            return MessageFormat.format(
                Messages.getString("UnknownCommandException.CommandUnknownCommandFileHintFormat"), //$NON-NLS-1$
                userText);
        } else {
            return MessageFormat.format(Messages.getString("UnknownCommandException.CommandUnknownFormat"), userText); //$NON-NLS-1$
        }
    }
}
