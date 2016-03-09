// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.exceptions;

import com.microsoft.tfs.core.exceptions.TEClientException;

/**
 * Base class for version control client exceptions.
 *
 * @since TEE-SDK-10.1
 */
public class VersionControlException extends TEClientException {
    private TeamFoundationServerExceptionProperties properties;

    public VersionControlException() {
        super();
    }

    public VersionControlException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public VersionControlException(final String message) {
        super(message);
    }

    public VersionControlException(final Throwable cause) {
        super(cause);
    }

    public TeamFoundationServerExceptionProperties getProperties() {
        if (properties == null) {
            properties = new TeamFoundationServerExceptionProperties();
        }

        return properties;
    }

    public void setProperties(final TeamFoundationServerExceptionProperties properties) {
        this.properties = properties;
    }
}
