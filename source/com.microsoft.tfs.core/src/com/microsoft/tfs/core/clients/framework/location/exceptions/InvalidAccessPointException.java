// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.framework.location.exceptions;

import com.microsoft.tfs.core.Messages;

/**
 * Exception raised when a non-fully qualified access mapping does not have a
 * defined access point.
 */
public class InvalidAccessPointException extends LocationException {
    public InvalidAccessPointException() {
        super(Messages.getString("InvalidAccessPointException.AccessMappingInvalid")); //$NON-NLS-1$
    }
}
