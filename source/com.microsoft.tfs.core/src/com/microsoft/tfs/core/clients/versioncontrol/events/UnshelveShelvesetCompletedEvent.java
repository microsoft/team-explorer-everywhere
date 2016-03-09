// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.events;

import com.microsoft.tfs.core.clients.versioncontrol.ProcessType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Shelveset;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;

/**
 * Event fired when an unshelve operation has completed.
 *
 * @since TEE-SDK-10.1
 */
public class UnshelveShelvesetCompletedEvent extends OperationCompletedEvent {
    private final Shelveset shelveset;
    private final PendingChange[] changes;

    public UnshelveShelvesetCompletedEvent(
        final EventSource source,
        final Workspace workspace,
        final Shelveset shelveset,
        final PendingChange[] changes) {
        super(source, workspace, ProcessType.UNSHELVE);

        this.shelveset = shelveset;
        this.changes = changes;
    }

    public Shelveset getShelveset() {
        return shelveset;
    }

    public PendingChange[] getChanges() {
        return changes;
    }
}
