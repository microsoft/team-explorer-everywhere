// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.events;

import com.microsoft.tfs.core.clients.versioncontrol.ProcessType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Conflict;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;

/**
 * Event fired when conflict resolution has started.
 *
 * @since TEE-SDK-10.1
 */
public class ResolveConflictsStartedEvent extends OperationStartedEvent {
    private final Conflict[] conflicts;

    public ResolveConflictsStartedEvent(
        final EventSource source,
        final Workspace workspace,
        final Conflict[] conflicts) {
        super(source, workspace, ProcessType.NONE);

        this.conflicts = conflicts;
    }

    public Conflict[] getConflicts() {
        return conflicts;
    }
}
