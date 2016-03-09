// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.exceptions;

import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ChangeRequest;

/**
 * Thrown when construction of an {@link ChangeRequest} fails a validation check
 * of the target item.
 *
 * @since TEE-SDK-10.1
 */
public final class ChangeRequestValidationException extends VersionControlException {
    public ChangeRequestValidationException() {
        super();
    }

    public ChangeRequestValidationException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public ChangeRequestValidationException(final String message) {
        super(message);
    }

    public ChangeRequestValidationException(final Throwable cause) {
        super(cause);
    }
}
