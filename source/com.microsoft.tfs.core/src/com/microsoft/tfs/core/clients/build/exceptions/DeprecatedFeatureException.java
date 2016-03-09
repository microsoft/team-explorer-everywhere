// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.exceptions;

import com.microsoft.tfs.core.Messages;

/**
 * Exception thrown when a deprecated feature was used.
 *
 * @since TEE-SDK-10.1
 */
public class DeprecatedFeatureException extends RuntimeException {

    public DeprecatedFeatureException() {
        super(Messages.getString("DeprecatedFeatureException.MessageFormat")); //$NON-NLS-1$
    }

    public DeprecatedFeatureException(final String message) {
        super(message);
    }

    public DeprecatedFeatureException(final Throwable cause) {
        super(cause);
    }

    public DeprecatedFeatureException(final String message, final Throwable cause) {
        super(message, cause);
    }

}
