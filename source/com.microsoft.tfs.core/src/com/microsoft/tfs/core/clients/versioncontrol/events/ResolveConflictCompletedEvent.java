// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.events;

import com.microsoft.tfs.core.clients.versioncontrol.ProcessType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Conflict;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;

/**
 * EVent fired when a conlict has been resolved.
 *
 * @since TEE-SDK-10.1
 */
public class ResolveConflictCompletedEvent extends OperationCompletedEvent {
    private final Conflict conflict;

    public ResolveConflictCompletedEvent(final EventSource source, final Workspace workspace, final Conflict conflict) {
        super(source, workspace, ProcessType.NONE);

        this.conflict = conflict;
    }

    public Conflict getConflict() {
        return conflict;
    }
}
