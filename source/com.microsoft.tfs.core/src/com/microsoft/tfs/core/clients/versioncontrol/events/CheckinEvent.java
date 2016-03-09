// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.events;

import com.microsoft.tfs.core.clients.CoreClientEvent;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.util.Check;

/**
 * Event fired when pending changes have been successfully checked in.
 *
 *
 * @since TEE-SDK-10.1
 */
public class CheckinEvent extends CoreClientEvent {
    static final long serialVersionUID = 7689380099998635255L;
    private final Workspace workspace;
    private final int changesetID;
    private final PendingChange[] committedChanges;
    private final PendingChange[] undoneChanges;

    public CheckinEvent(
        final EventSource source,
        final Workspace workspace,
        final int changesetID,
        final PendingChange[] committedChanges,
        final PendingChange[] undoneChanges) {
        super(source);

        Check.notNull(source, "source"); //$NON-NLS-1$
        Check.notNull(workspace, "workspace"); //$NON-NLS-1$
        Check.notNull(committedChanges, "committedChanges"); //$NON-NLS-1$
        Check.notNull(undoneChanges, "undoneChanges"); //$NON-NLS-1$

        this.workspace = workspace;
        this.changesetID = changesetID;
        this.committedChanges = committedChanges.clone();
        this.undoneChanges = undoneChanges.clone();
    }

    public Workspace getWorkspace() {
        return workspace;
    }

    public int getChangesetID() {
        return changesetID;
    }

    public PendingChange[] getCommittedChanges() {
        return committedChanges;
    }

    public PendingChange[] getUndoneChanges() {
        return undoneChanges;
    }
}
