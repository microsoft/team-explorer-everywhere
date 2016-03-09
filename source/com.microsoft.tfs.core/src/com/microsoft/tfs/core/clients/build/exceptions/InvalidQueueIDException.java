// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.exceptions;

import java.text.MessageFormat;

import com.microsoft.tfs.core.Messages;

/**
 * Exception thrown when an invalid queue ID was specified.
 *
 * @since TEE-SDK-10.1
 */
public class InvalidQueueIDException extends BuildException {
    private final int id;

    public InvalidQueueIDException(final int id) {
        super(MessageFormat.format(Messages.getString("InvalidQueueIdException.MessageFormat"), Integer.toString(id))); //$NON-NLS-1$
        this.id = id;
    }

    /**
     * @return the id
     */
    public int getID() {
        return id;
    }

}
