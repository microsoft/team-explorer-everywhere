// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.views;

import com.microsoft.tfs.client.common.repository.RepositoryManagerAdapter;
import com.microsoft.tfs.client.common.repository.RepositoryManagerEvent;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.teamexplorer.ProjectAndTeamListener;
import com.microsoft.tfs.client.common.ui.teamexplorer.TeamExplorerContext;
import com.microsoft.tfs.core.ConnectivityFailureStatusChangeListener;
import com.microsoft.tfs.core.clients.versioncontrol.events.WorkspaceUpdatedEvent;
import com.microsoft.tfs.core.clients.versioncontrol.events.WorkspaceUpdatedListener;

/**
 * The repository listener for all dockable views.
 */
public class UndockedViewRepositoryListener extends RepositoryManagerAdapter implements ProjectAndTeamListener {

    private final TeamExplorerContext context;
    private final TeamExplorerDockableView view;

    public UndockedViewRepositoryListener(final TeamExplorerDockableView view) {
        this.view = view;
        this.context = view.getContext();
    }

    public UndockedViewRepositoryListener(final TeamExplorerDockableView view, final TFSRepository repository) {
        this(view);
        if (repository != null) {
            addRepositoryListeners(repository);
        }
    }

    private class UpdatedListener implements WorkspaceUpdatedListener {
        @Override
        public void onWorkspaceUpdated(final WorkspaceUpdatedEvent e) {
            /*
             * Refresh the control if the workspace location changed.
             *
             * The original location will be null when this event comes from
             * IPC, so don't consider that a change.
             */
            if (e.getOriginalLocation() != null && e.getOriginalLocation() != e.getWorkspace().getLocation()) {
                context.refresh();
                view.refresh();
            }
        }
    }

    private class CoreConnectivityFailureStatusChangeListener implements ConnectivityFailureStatusChangeListener {
        @Override
        public void onConnectivityFailureStatusChange() {
            context.refresh();
            view.refresh();
        }
    }

    private final UpdatedListener updatedListener = new UpdatedListener();
    private final ConnectivityFailureStatusChangeListener connectivityListener =
        new CoreConnectivityFailureStatusChangeListener();

    public void addRepositoryListeners(final TFSRepository repository) {
        repository.getVersionControlClient().getEventEngine().addWorkspaceUpdatedListener(updatedListener);
        repository.getConnection().addConnectivityFailureStatusChangeListener(connectivityListener);
    }

    public void removeRepositoryListeners(final TFSRepository repository) {
        repository.getVersionControlClient().getEventEngine().removeWorkspaceUpdatedListener(updatedListener);
        repository.getConnection().removeConnectivityFailureStatusChangeListener(connectivityListener);
    }

    @Override
    public void onRepositoryAdded(final RepositoryManagerEvent event) {
        addRepositoryListeners(event.getRepository());
    }

    @Override
    public void onRepositoryRemoved(final RepositoryManagerEvent event) {
        removeRepositoryListeners(event.getRepository());
    }

    @Override
    public void onDefaultRepositoryChanged(final RepositoryManagerEvent event) {
        context.refresh();
        view.refresh();
    }

    @Override
    public void onProjectOrTeamChanged() {
        context.refresh();
        view.refresh();
    }
}
