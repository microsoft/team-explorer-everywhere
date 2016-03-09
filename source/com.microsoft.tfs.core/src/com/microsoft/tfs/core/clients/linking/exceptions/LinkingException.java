// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.linking.exceptions;

import com.microsoft.tfs.core.exceptions.TEClientException;

/**
 * Base class for linking client exceptions.
 *
 * @since TEE-SDK-10.1
 */
public class LinkingException extends TEClientException {
    public LinkingException() {
        super();
    }

    public LinkingException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public LinkingException(final String message) {
        super(message);
    }

    public LinkingException(final Throwable cause) {
        super(cause);
    }
}
