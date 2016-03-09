// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.framework.location.exceptions;

import java.text.MessageFormat;

import com.microsoft.tfs.core.Messages;

/**
 * Exception raised when an unimplemented location service method is called.
 */
public class LocationMethodNotImplementedException extends LocationException {
    public LocationMethodNotImplementedException(final String methodName) {
        super(
            MessageFormat.format(
                Messages.getString("LocationMethodNotImplementedException.LocationServiceMethodNotImplementedFormat"), //$NON-NLS-1$
                methodName));
    }
}
