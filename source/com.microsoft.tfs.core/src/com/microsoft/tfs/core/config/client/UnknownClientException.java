// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.config.client;

import java.text.MessageFormat;

import com.microsoft.tfs.core.exceptions.TECoreException;

/**
 * Thrown by a {@link ClientFactory} when it cannot create a client for the
 * requested type.
 *
 * @since TEE-SDK-10.1
 * @threadsafety thread-safe
 */
public class UnknownClientException extends TECoreException {
    public UnknownClientException(final Class clientType) {
        super(MessageFormat.format("Unknown client type: {0}", clientType.getName())); //$NON-NLS-1$
    }
}
