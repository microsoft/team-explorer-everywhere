// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.events;

import com.microsoft.tfs.core.clients.CoreClientEvent;

/**
 * @since TEE-SDK-11.0
 */
public class ChangesetReconciledEvent extends CoreClientEvent {
    private final int changesetID;

    public ChangesetReconciledEvent(final EventSource source, final int changesetID) {
        super(source);

        this.changesetID = changesetID;
    }

    /**
     * The changeset that was reconciled.
     */
    public int getChangesetID() {
        return changesetID;
    }
}
