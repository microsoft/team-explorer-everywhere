// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.exceptions;

import com.microsoft.tfs.core.exceptions.TEClientException;

/**
 * Base class for build client exceptions.
 *
 * @since TEE-SDK-10.1
 */
public class BuildException extends TEClientException {
    public BuildException() {
        super();
    }

    public BuildException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public BuildException(final String message) {
        super(message);
    }

    public BuildException(final Throwable cause) {
        super(cause);
    }

}
