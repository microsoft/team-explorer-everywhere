// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.events;

import com.microsoft.tfs.core.clients.CoreClientEvent;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlClient;

/**
 * @since TEE-SDK-11.0
 */
public class FolderContentChangedEvent extends CoreClientEvent {
    private final VersionControlClient client;
    private final int changesetID;

    public FolderContentChangedEvent(
        final EventSource source,
        final VersionControlClient client,
        final int changesetID) {
        super(source);

        this.client = client;
        this.changesetID = changesetID;
    }

    /**
     * @return the client where this folder data changed (may be
     *         <code>null</code>)
     */
    public VersionControlClient getClient() {
        return client;
    }

    /**
     * If available, the Id of the changeset in which the changes were
     * committed. An Id value of -1 indicates that the changeset is not
     * available like in the case of items destroyed.
     */
    public int getChangesetID() {
        return changesetID;
    }
}
