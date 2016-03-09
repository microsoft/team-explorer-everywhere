// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.repository;

import com.microsoft.tfs.core.clients.versioncontrol.events.WorkspaceEvent.WorkspaceEventSource;

/**
 * Listener that will be notified when a {@link TFSRepository}'s internal
 * {@link Workspace} is updated by internal or external processes.
 *
 * @threadsafety unknown
 */
public interface TFSRepositoryUpdatedListener {
    /**
     * Called when the repository's workspace changes (workspace renamed,
     * mappings changed, etc.).
     */
    void onRepositoryUpdated();

    /**
     * Called when another client has made changes to the repository which need
     * to be reflected in SCE or other views.
     *
     * @param changesetID
     *        the changeset that was created when the change was made, possibly
     *        -1 to indicate no new changeset was created
     */
    void onFolderContentChanged(int changesetID);

    /**
     * Called when another client has completed a get that might have affected
     * items displayed in SCE or other views.
     *
     * @param source
     *        the source of the event (must not be <code>null</code>)
     */
    void onGetCompletedEvent(WorkspaceEventSource source);

    /**
     * Called when another client has detected a change to an item in a local
     * workspace.
     *
     * @param source
     *        the source of the event (must not be <code>null</code>)
     */
    void onLocalWorkspaceScan(WorkspaceEventSource source);
}
