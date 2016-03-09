// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.exceptions;

import java.text.MessageFormat;

import com.microsoft.tfs.core.clients.framework.location.exceptions.LocationException;

/**
 * Exception raised when an unimplemented framework service method is called.
 *
 * @since TEE-SDK-10.1
 */
public class FrameworkMethodNotImplementedException extends LocationException {
    public FrameworkMethodNotImplementedException(final String methodName) {
        super(MessageFormat.format("Framework method '{0}' is not implemented.", methodName)); //$NON-NLS-1$
    }
}
