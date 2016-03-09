// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.exceptions;

import java.text.MessageFormat;

import com.microsoft.tfs.core.Messages;

/**
 * Thrown when a local workspace properties table cannot be loaded.
 *
 * @since TEE-SDK-11.0
 */
public final class UnableToLoadLocalPropertiesTableException extends VersionControlException {
    public UnableToLoadLocalPropertiesTableException(final String workspaceName, final Throwable cause) {
        super(
            MessageFormat.format(
                Messages.getString("UnableToLoadLocalPropertiesTableException.UnableToLoadLocalPropertiesTableFormat"), //$NON-NLS-1$
                workspaceName,
                cause.getLocalizedMessage()),
            cause);
    }
}
