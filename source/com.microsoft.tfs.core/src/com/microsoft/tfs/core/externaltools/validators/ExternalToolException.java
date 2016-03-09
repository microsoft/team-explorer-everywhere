// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.externaltools.validators;

import com.microsoft.tfs.core.exceptions.TECoreException;
import com.microsoft.tfs.core.externaltools.ExternalTool;

/**
 * Thrown when arguments to an {@link ExternalTool} are invalid, or some other
 * error happened configuring the {@link ExternalTool}.
 *
 * @since TEE-SDK-10.1
 * @threadsafety thread-safe
 */
public final class ExternalToolException extends TECoreException {
    public ExternalToolException(final String message) {
        super(message);
    }
}
