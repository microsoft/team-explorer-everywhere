// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.commands;

import java.text.MessageFormat;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.microsoft.tfs.client.common.Messages;
import com.microsoft.tfs.core.clients.versioncontrol.WorkspaceLocation;
import com.microsoft.tfs.core.clients.versioncontrol.WorkspaceOptions;
import com.microsoft.tfs.core.clients.versioncontrol.WorkspacePermissionProfile;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.WorkingFolder;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.LocaleUtil;

public class UpdateWorkspaceCommand extends TFSConnectedCommand {
    private final Workspace workspace;
    private final String newWorkspaceName;
    private final String newComment;
    private final WorkingFolder[] newWorkingFolders;
    private final WorkspaceOptions newOptions;
    private final WorkspaceLocation newLocation;
    private final WorkspacePermissionProfile permissionprofile;

    /**
     * Pass <code>null</code> for values you don't wish to change. The
     * {@link Workspace} cannot be <code>null</code>.
     */
    public UpdateWorkspaceCommand(
        final Workspace workspace,
        final String newWorkspaceName,
        final String newComment,
        final WorkingFolder[] newWorkingFolders,
        final WorkspaceOptions newOptions,
        final WorkspaceLocation newLocation,
        final WorkspacePermissionProfile permissionprofile) {
        Check.notNull(workspace, "workspace"); //$NON-NLS-1$

        this.workspace = workspace;
        this.newWorkspaceName = newWorkspaceName;
        this.newComment = newComment;
        this.newWorkingFolders = newWorkingFolders;
        this.newOptions = newOptions;
        this.newLocation = newLocation;
        this.permissionprofile = permissionprofile;

        setCancellable(true);
        setConnection(workspace.getClient().getConnection());
    }

    @Override
    public String getName() {
        final String messageFormat = Messages.getString("UpdateWorkspaceCommand.CommandTextFormat"); //$NON-NLS-1$
        return MessageFormat.format(messageFormat, workspace.getName());
    }

    @Override
    public String getErrorDescription() {
        return (Messages.getString("UpdateWorkspaceCommand.ErrorText")); //$NON-NLS-1$
    }

    @Override
    public String getLoggingDescription() {
        final String messageFormat = Messages.getString("UpdateWorkspaceCommand.CommandTextFormat", LocaleUtil.ROOT); //$NON-NLS-1$
        return MessageFormat.format(messageFormat, workspace.getName());
    }

    @Override
    protected IStatus doRun(final IProgressMonitor progressMonitor) throws Exception {
        final String messageFormat = Messages.getString("UpdateWorkspaceCommand.ProgressMonitorTextFormat"); //$NON-NLS-1$
        final String message = MessageFormat.format(messageFormat, workspace.getName());
        progressMonitor.beginTask(message, IProgressMonitor.UNKNOWN);

        workspace.update(
            newWorkspaceName,
            null,
            newComment,
            null,
            newWorkingFolders,
            permissionprofile,
            false,
            newOptions,
            newLocation);

        return Status.OK_STATUS;
    }
}
