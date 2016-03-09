// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.checkinpolicies;

import com.microsoft.tfs.core.clients.versioncontrol.exceptions.VersionControlException;
import com.microsoft.tfs.core.memento.Memento;

/**
 * <p>
 * Thrown when a {@link PolicyDefinition} encounters a problem saving to or
 * loading from a {@link Memento}.
 * </p>
 *
 * @since TEE-SDK-10.1
 * @threadsafety thread-safe
 */
public class PolicySerializationException extends VersionControlException {
    public PolicySerializationException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public PolicySerializationException(final String message) {
        super(message);
    }

    public PolicySerializationException(final Throwable cause) {
        super(cause);
    }
}
