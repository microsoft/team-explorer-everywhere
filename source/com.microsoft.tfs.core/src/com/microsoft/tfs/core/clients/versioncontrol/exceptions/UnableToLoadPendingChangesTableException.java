// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.exceptions;

import java.text.MessageFormat;

import com.microsoft.tfs.core.Messages;

/**
 * Thrown when a local workspace pending changes table cannot be loaded.
 *
 * @since TEE-SDK-11.0
 */
public final class UnableToLoadPendingChangesTableException extends VersionControlException {
    public UnableToLoadPendingChangesTableException(final String workspaceName, final Throwable cause) {
        super(
            MessageFormat.format(
                Messages.getString("UnableToLoadPendingChangesTableException.UnableToLoadPendingChangesTableFormat"), //$NON-NLS-1$
                workspaceName),
            cause);
    }
}
