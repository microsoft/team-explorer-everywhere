// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.exceptions;

/**
 * {@link FeatureNotSupportedException} is thrown by version control classes
 * when a required feature is not supported by a Team Foundation server.
 *
 * @since TEE-SDK-10.1
 */
public class FeatureNotSupportedException extends VersionControlException {
    /**
     * Creates a new {@link FeatureNotSupportedException} with the specified
     * message.
     *
     * @param message
     *        the exception message
     */
    public FeatureNotSupportedException(final String message) {
        super(message);
    }
}
