// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.exceptions;

import java.text.MessageFormat;
import java.util.List;

import com.microsoft.tfs.core.Messages;

/**
 * An exception thrown when multiple workspaces that match search criteria are
 * found in the workspace cache.
 *
 * @since TEE-SDK-11.0
 */
public final class MultipleWorkspacesFoundException extends VersionControlException {
    public MultipleWorkspacesFoundException(
        final String suppliedWorkspaceName,
        final String suppliedOwnerName,
        final List<String> matchingWorkspaceSpecs) {
        super(MessageFormat.format(
            Messages.getString("MultipleWorkspacesFoundException.MessageFormat"), //$NON-NLS-1$
            suppliedWorkspaceName,
            suppliedOwnerName,
            matchingWorkspaceSpecs));
    }
}
