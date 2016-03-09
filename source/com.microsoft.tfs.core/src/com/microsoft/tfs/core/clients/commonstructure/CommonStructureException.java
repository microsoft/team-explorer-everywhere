// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.commonstructure;

import com.microsoft.tfs.core.exceptions.TEClientException;

/**
 * Base class for {@link CommonStructureClient} exceptions.
 *
 * @since TEE-SDK-10.1
 */
public class CommonStructureException extends TEClientException {
    public CommonStructureException() {
        super();
    }

    public CommonStructureException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public CommonStructureException(final String message) {
        super(message);
    }

    public CommonStructureException(final Throwable cause) {
        super(cause);
    }
}
