// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.exceptions;

import java.text.MessageFormat;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;

/**
 * Exception thrown when items in multiple workspaces are passed to a method
 * which can only operate on items in a single workspace.
 *
 * @since TEE-SDK-10.1
 */
public class OnlyOneWorkspaceException extends VersionControlException {
    public OnlyOneWorkspaceException(final Workspace workspace, final String path) {
        this(workspace.getName(), path);
    }

    public OnlyOneWorkspaceException(final String workspaceName, final String path) {
        super(MessageFormat.format(
            //@formatter:off
            Messages.getString("OnlyOneWorkspaceException.AllSpecifiedFilesMustResideInTheSameWorkspaceWorkspaceContainsPathFormat"), //$NON-NLS-1$
            //@formatter:on
            workspaceName,
            path));
    }
}
