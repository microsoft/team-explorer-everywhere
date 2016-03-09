// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.vc;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Comparator;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.services.IDisposable;

import com.microsoft.tfs.client.common.commands.QueryLocalWorkspacesCommand;
import com.microsoft.tfs.client.common.connectionconflict.ConnectionConflictHandler;
import com.microsoft.tfs.client.common.framework.command.CommandExecutor;
import com.microsoft.tfs.client.common.repository.RepositoryConflictException;
import com.microsoft.tfs.client.common.repository.RepositoryManager;
import com.microsoft.tfs.client.common.repository.RepositoryManagerEvent;
import com.microsoft.tfs.client.common.repository.RepositoryManagerListener;
import com.microsoft.tfs.client.common.server.ServerManagerEvent;
import com.microsoft.tfs.client.common.server.ServerManagerListener;
import com.microsoft.tfs.client.common.server.TFSServer;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.framework.action.ToolbarPulldownAction;
import com.microsoft.tfs.client.common.ui.framework.helper.UIHelpers;
import com.microsoft.tfs.client.common.ui.helpers.TFSTeamProjectCollectionFormatter;
import com.microsoft.tfs.client.common.ui.helpers.UIConnectionPersistence;
import com.microsoft.tfs.client.common.ui.teamexplorer.helpers.PendingChangesHelpers;
import com.microsoft.tfs.core.clients.versioncontrol.events.WorkspaceCreatedListener;
import com.microsoft.tfs.core.clients.versioncontrol.events.WorkspaceDeletedListener;
import com.microsoft.tfs.core.clients.versioncontrol.events.WorkspaceEvent;
import com.microsoft.tfs.core.clients.versioncontrol.events.WorkspaceUpdatedEvent;
import com.microsoft.tfs.core.clients.versioncontrol.events.WorkspaceUpdatedListener;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.core.clients.versioncontrol.specs.WorkspaceSpec;
import com.microsoft.tfs.core.exceptions.TECoreException;
import com.microsoft.tfs.util.Check;

public class WorkspaceToolbarPulldownAction extends ToolbarPulldownAction implements IDisposable {
    private final Shell shell;
    private final IAction manageWorkspacesAction;

    private final WorkspaceComboConnectionListener connectionListener = new WorkspaceComboConnectionListener();
    private final WorkspaceComboWorkspaceListener workspaceListener = new WorkspaceComboWorkspaceListener();

    private boolean disposed = false;

    /* Note: all fields below must only be accessed on the UI thread. */
    private Job updateJob = null;
    private Workspace[] workspaces;
    private Workspace currentWorkspace;

    public WorkspaceToolbarPulldownAction(final Shell shell) {
        super(true);
        this.shell = shell;

        // Create the manage workspace action.
        manageWorkspacesAction = new Action() {
            @Override
            public void run() {
                manageWorkspaces();
            }
        };
        manageWorkspacesAction.setText(Messages.getString("WorkspaceToolbarPulldownAction.ManageWorkspacesActionText")); //$NON-NLS-1$

        final TFSServer server =
            TFSCommonUIClientPlugin.getDefault().getProductPlugin().getServerManager().getDefaultServer();

        if (server != null) {
            server.getConnection().getVersionControlClient().getEventEngine().addWorkspaceCreatedListener(
                workspaceListener);
            server.getConnection().getVersionControlClient().getEventEngine().addWorkspaceDeletedListener(
                workspaceListener);
            server.getConnection().getVersionControlClient().getEventEngine().addWorkspaceUpdatedListener(
                workspaceListener);
        }

        TFSCommonUIClientPlugin.getDefault().getProductPlugin().getServerManager().addListener(connectionListener);
        TFSCommonUIClientPlugin.getDefault().getProductPlugin().getRepositoryManager().addListener(connectionListener);

        repopulate();
    }

    @Override
    public void dispose() {
        TFSCommonUIClientPlugin.getDefault().getProductPlugin().getServerManager().removeListener(connectionListener);
        TFSCommonUIClientPlugin.getDefault().getProductPlugin().getRepositoryManager().removeListener(
            connectionListener);
        disposed = true;
    }

    public void setCurrentWorkspace(final Workspace workspace) {
        currentWorkspace = workspace;
        final String tooltipFormat =
            Messages.getString("WorkspaceToolbarPulldownAction.WorkspaceButtonTooltipTextFormat"); //$NON-NLS-1$

        if (workspace != null) {
            setText(workspace.getName());
            setToolTipText(MessageFormat.format(tooltipFormat, workspace.getName()));
        } else {
            setText(Messages.getString("WorkspaceToolbarPulldownAction.NotConnectedMessage")); //$NON-NLS-1$
            setToolTipText(
                MessageFormat.format(
                    tooltipFormat,
                    Messages.getString("WorkspaceToolbarPulldownAction.NotConnectedMessage"))); //$NON-NLS-1$
        }
    }

    @Override
    protected Menu getSubActionMenu(final Control parent) {
        for (final IAction action : getSubActions()) {
            if (currentWorkspace != null && action instanceof SwitchToWorkspaceAction) {
                final SwitchToWorkspaceAction switchAction = (SwitchToWorkspaceAction) action;
                final boolean match =
                    Workspace.matchName(switchAction.getWorkspace().getName(), currentWorkspace.getName());
                action.setChecked(match);
            }
        }
        return super.getSubActionMenu(parent);
    }

    private void manageWorkspaces() {
        PendingChangesHelpers.manageWorkspaces(shell);
        repopulate();
    }

    private void switchWorkspace(final Workspace workspace) {
        final RepositoryManager repositoryManager =
            TFSCommonUIClientPlugin.getDefault().getProductPlugin().getRepositoryManager();

        final ConnectionConflictHandler connectionConflictHandler =
            TFSCommonUIClientPlugin.getDefault().getProductPlugin().getConnectionConflictHandler();

        /*
         * Ensure the workspace exists. It could have been deleted by another
         * program since it was added to this combo box.
         */
        try {
            TFSCommonUIClientPlugin.getDefault().getProductPlugin().getServerManager().getDefaultServer().getConnection().getVersionControlClient().queryWorkspace(
                workspace.getName(),
                workspace.getOwnerName());
        } catch (final TECoreException e) {
            final String message =
                MessageFormat.format(
                    Messages.getString("WorkspaceToolbarPulldownAction.WorkspaceDoesNotExistFormat"), //$NON-NLS-1$
                    new WorkspaceSpec(workspace.getOwnerName(), workspace.getOwnerDisplayName()).toString());

            ErrorDialog.openError(
                shell,
                Messages.getString("WorkspaceToolbarPulldownAction.WorkspaceNotFound"), //$NON-NLS-1$
                null,
                new Status(Status.WARNING, TFSCommonUIClientPlugin.PLUGIN_ID, 0, message, null));

            repopulate();

            return;
        }

        /*
         * Switch to the selected workspace.
         */
        try {
            repositoryManager.getOrCreateRepository(workspace);
        } catch (final RepositoryConflictException conflictException) {
            /*
             * Another connection to a server already exists: allow the product
             * plugin's connection conflict handler to retry this.
             */
            if (connectionConflictHandler.resolveRepositoryConflict()) {
                /* Retry */
                try {
                    repositoryManager.getOrCreateRepository(workspace);
                } catch (final RepositoryConflictException f) {
                    connectionConflictHandler.notifyRepositoryConflict();
                }
            }
        }

        UIConnectionPersistence.getInstance().setLastUsedWorkspace(workspace);
    }

    private void repopulate() {
        if (disposed) {
            return;
        }

        final TFSServer server =
            TFSCommonUIClientPlugin.getDefault().getProductPlugin().getServerManager().getDefaultServer();

        if (server == null) {
            removeAll();
        } else {
            removeAll();

            updateJob = new UpdateWorkspaceComboJob(server);
            updateJob.schedule();
        }
    }

    private class UpdateWorkspaceComboJob extends Job {
        private final TFSServer server;

        public UpdateWorkspaceComboJob(final TFSServer server) {
            super(MessageFormat.format(
                Messages.getString("UpdateWorkspaceComboJob.NameFormat"), //$NON-NLS-1$
                TFSTeamProjectCollectionFormatter.getLabel(server.getConnection())));

            Check.notNull(server, "server"); //$NON-NLS-1$

            this.server = server;
        }

        @Override
        protected IStatus run(final IProgressMonitor monitor) {
            final QueryLocalWorkspacesCommand queryCommand = new QueryLocalWorkspacesCommand(server.getConnection());
            final IStatus queryStatus = new CommandExecutor().execute(queryCommand);

            if (!queryStatus.isOK()) {
                return queryStatus;
            }

            final Workspace[] workspaces = queryCommand.getWorkspaces();

            Arrays.sort(workspaces, new Comparator<Workspace>() {
                @Override
                public int compare(final Workspace workspace0, final Workspace workspace1) {
                    return String.CASE_INSENSITIVE_ORDER.compare(workspace0.getName(), workspace1.getName());
                }
            });

            UIHelpers.runOnUIThread(true, new Runnable() {
                @Override
                public void run() {
                    /*
                     * Another connection could have triggered this repopulate -
                     * make sure that we're still the most recent job running.
                     * If not, defer this update.
                     */
                    if (updateJob != UpdateWorkspaceComboJob.this) {
                        return;
                    }

                    WorkspaceToolbarPulldownAction.this.workspaces = workspaces;
                    final ToolbarPulldownAction pulldownAction = WorkspaceToolbarPulldownAction.this;
                    pulldownAction.removeAll();

                    for (final Workspace workspace : workspaces) {
                        final SwitchToWorkspaceAction subAction = new SwitchToWorkspaceAction(workspace);
                        pulldownAction.addSubAction(subAction);
                    }

                    pulldownAction.addSubAction(manageWorkspacesAction);
                }
            });

            return Status.OK_STATUS;
        }
    }

    private final class SwitchToWorkspaceAction extends Action {
        private final Workspace workspace;

        public SwitchToWorkspaceAction(final Workspace workspace) {
            super(workspace.getName(), IAction.AS_CHECK_BOX);
            this.workspace = workspace;
        }

        public Workspace getWorkspace() {
            return workspace;
        }

        @Override
        public void run() {
            switchWorkspace(workspace);
        }
    }

    private final class WorkspaceComboConnectionListener implements ServerManagerListener, RepositoryManagerListener {
        @Override
        public void onRepositoryAdded(final RepositoryManagerEvent event) {
            /* Ignore */
        }

        @Override
        public void onRepositoryRemoved(final RepositoryManagerEvent event) {
            /* Ignore */
        }

        @Override
        public void onDefaultRepositoryChanged(final RepositoryManagerEvent event) {
            UIHelpers.runOnUIThread(true, new Runnable() {
                @Override
                public void run() {
                    /* Wait for the server to get hooked up */
                    if (workspaces == null) {
                        return;
                    }
                }
            });
        }

        @Override
        public void onServerAdded(final ServerManagerEvent event) {
            event.getServer().getConnection().getVersionControlClient().getEventEngine().addWorkspaceCreatedListener(
                workspaceListener);
            event.getServer().getConnection().getVersionControlClient().getEventEngine().addWorkspaceDeletedListener(
                workspaceListener);
            event.getServer().getConnection().getVersionControlClient().getEventEngine().addWorkspaceUpdatedListener(
                workspaceListener);
        }

        @Override
        public void onServerRemoved(final ServerManagerEvent event) {
            event.getServer().getConnection().getVersionControlClient().getEventEngine().removeWorkspaceCreatedListener(
                workspaceListener);
            event.getServer().getConnection().getVersionControlClient().getEventEngine().removeWorkspaceDeletedListener(
                workspaceListener);
            event.getServer().getConnection().getVersionControlClient().getEventEngine().removeWorkspaceUpdatedListener(
                workspaceListener);
        }

        @Override
        public void onDefaultServerChanged(final ServerManagerEvent event) {
            UIHelpers.runOnUIThread(true, new Runnable() {
                @Override
                public void run() {
                    repopulate();
                }
            });
        }
    }

    private final class WorkspaceComboWorkspaceListener
        implements WorkspaceCreatedListener, WorkspaceUpdatedListener, WorkspaceDeletedListener {
        @Override
        public void onWorkspaceCreated(final WorkspaceEvent e) {
            repopulateUI();
        }

        @Override
        public void onWorkspaceUpdated(final WorkspaceUpdatedEvent e) {
            repopulateUI();
        }

        @Override
        public void onWorkspaceDeleted(final WorkspaceEvent e) {
            repopulateUI();
        }

        private void repopulateUI() {
            UIHelpers.runOnUIThread(true, new Runnable() {
                @Override
                public void run() {
                    repopulate();
                }
            });
        }
    }
}
