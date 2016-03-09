// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.events;

import com.microsoft.tfs.core.clients.CoreClientEvent;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Shelveset;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.util.Check;

/**
 * Event fired when pending changes have been successfully shelved.
 *
 *
 * @since TEE-SDK-10.1
 */
public class ShelveEvent extends CoreClientEvent {
    private final Workspace workspace;
    private final Shelveset shelveset;
    private final PendingChange[] shelvedChanges;
    private final boolean move;

    public ShelveEvent(
        final EventSource source,
        final Workspace workspace,
        final Shelveset shelveset,
        final PendingChange[] shelvedChanges,
        final boolean move) {
        super(source);

        Check.notNull(source, "source"); //$NON-NLS-1$
        Check.notNull(workspace, "workspace"); //$NON-NLS-1$
        Check.notNull(shelveset, "shelveset"); //$NON-NLS-1$
        Check.notNull(shelvedChanges, "shelvedChanges"); //$NON-NLS-1$

        this.workspace = workspace;
        this.shelveset = shelveset;
        this.shelvedChanges = shelvedChanges;
        this.move = move;
    }

    public Workspace getWorkspace() {
        return workspace;
    }

    public Shelveset getShelveset() {
        return shelveset;
    }

    public PendingChange[] getShelvedChanges() {
        return shelvedChanges;
    }

    public boolean isMove() {
        return move;
    }
}
