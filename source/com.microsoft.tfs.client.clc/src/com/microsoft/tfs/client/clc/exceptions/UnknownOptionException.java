// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.clc.exceptions;

import java.text.MessageFormat;

import com.microsoft.tfs.client.clc.Messages;

/**
 *         Thrown when an option is parsed, but the name does not match any
 *         known option.
 */
public final class UnknownOptionException extends ArgumentException {
    static final long serialVersionUID = 3738441317127684192L;

    public UnknownOptionException() {
        super();
    }

    public UnknownOptionException(final String userText) {
        super(MessageFormat.format(Messages.getString("UnknownOptionException.UnknownOptionFormat"), userText)); //$NON-NLS-1$
    }
}
