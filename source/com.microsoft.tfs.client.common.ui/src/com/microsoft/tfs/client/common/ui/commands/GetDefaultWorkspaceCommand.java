// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.commands;

import java.text.MessageFormat;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.microsoft.tfs.client.common.commands.TFSCommand;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.helpers.TFSTeamProjectCollectionFormatter;
import com.microsoft.tfs.client.common.ui.helpers.UIConnectionPersistence;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlConstants;
import com.microsoft.tfs.core.clients.versioncontrol.WorkspacePermissionProfile;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.jni.helpers.LocalHost;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.LocaleUtil;

/**
 * Gets the "default" TFS workspace (typically the last used workspace) for the
 * given server.
 *
 * If there is not a previously-used TFS workspace (or it cannot be realized),
 * then the workspace with the given local host name will be used. If that does
 * not exist, then the first workspace from the server will be used (on the
 * assumption that the user can easily change it.) Finally, if no workspaces
 * exist on the server at all, a new one will be created with the current
 * (short, non-FQDN) local host name.
 *
 * The repository connected will NOT be registered in the repository manager,
 * and the {@link TFSTeamProjectCollection} will NOT be registered in the server
 * manager.
 *
 * @threadsafety unknown
 */
public class GetDefaultWorkspaceCommand extends TFSCommand {
    private final TFSTeamProjectCollection connection;

    private Workspace workspace;

    public GetDefaultWorkspaceCommand(final TFSTeamProjectCollection connection) {
        Check.notNull(connection, "connection"); //$NON-NLS-1$

        this.connection = connection;

        setCancellable(true);
    }

    @Override
    public String getName() {
        return MessageFormat.format(
            Messages.getString("GetDefaultRepositoryCommand.CommandNameFormat"), //$NON-NLS-1$
            TFSTeamProjectCollectionFormatter.getLabel(connection));
    }

    @Override
    public String getErrorDescription() {
        return MessageFormat.format(
            Messages.getString("GetDefaultRepositoryCommand.CommandErrorTextFormat"), //$NON-NLS-1$
            TFSTeamProjectCollectionFormatter.getLabel(connection));
    }

    @Override
    public String getLoggingDescription() {
        return MessageFormat.format(
            Messages.getString("GetDefaultRepositoryCommand.CommandNameFormat", LocaleUtil.ROOT), //$NON-NLS-1$
            TFSTeamProjectCollectionFormatter.getLabel(connection));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected IStatus doRun(final IProgressMonitor progressMonitor) throws Exception {
        final String machineName = LocalHost.getShortName();

        final Workspace[] workspaces = connection.getVersionControlClient().queryWorkspaces(
            null,
            VersionControlConstants.AUTHENTICATED_USER,
            null);

        checkForCancellation(progressMonitor);

        if (workspaces == null || workspaces.length == 0) {
            // We have no workspaces - create a default one.

            workspace = connection.getVersionControlClient().createWorkspace(
                null,
                machineName,
                null,
                null,
                null,
                WorkspacePermissionProfile.getPrivateProfile());
        }

        /*
         * There were more than one workspace(s) on the server, try to use the
         * last-used workspace on this machine.
         */
        final String lastWorkspaceName = UIConnectionPersistence.getInstance().getLastUsedWorkspace(connection);

        if (workspace == null && lastWorkspaceName != null) {
            workspace = findWorkspace(lastWorkspaceName, machineName, workspaces);
        }

        /*
         * There was no last-used workspace, try to find one with the default
         * (i.e. machine) name on this machine.
         */
        if (workspace == null) {
            workspace = findWorkspace(machineName, machineName, workspaces);
        }

        /*
         * Still no luck - just pick the first workspace on this machine - it's
         * easy to change.
         */
        if (workspace == null) {
            workspace = findWorkspace(null, machineName, workspaces);
        }

        /*
         * Only remote workspaces are available. We have to create a new local
         * one, but its name has to be unique
         */
        if (workspace == null) {
            final String newWorkspaceName = Workspace.computeNewWorkspaceName(machineName, workspaces);
            workspace = connection.getVersionControlClient().createWorkspace(
                null,
                newWorkspaceName,
                null,
                null,
                null,
                WorkspacePermissionProfile.getPrivateProfile());
        }

        checkForCancellation(progressMonitor);

        return Status.OK_STATUS;
    }

    public Workspace getWorkspace() {
        return workspace;
    }

    private Workspace findWorkspace(final String name, final String machineName, final Workspace[] workspaces) {
        for (final Workspace workspace : workspaces) {
            if (name == null || workspace.getName().equalsIgnoreCase(name)) {
                if (machineName == null || workspace.getComputer().equalsIgnoreCase(machineName)) {
                    return workspace;
                }
            }
        }
        return null;
    }
}
