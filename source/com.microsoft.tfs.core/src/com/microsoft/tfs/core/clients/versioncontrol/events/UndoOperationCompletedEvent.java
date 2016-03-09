// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.events;

import com.microsoft.tfs.core.clients.versioncontrol.ProcessType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;
import com.microsoft.tfs.util.Check;

/**
 * Event fired when an undo operation has completed.
 *
 * @since TEE-SDK-10.1
 */
public class UndoOperationCompletedEvent extends OperationCompletedEvent {
    private final ItemSpec[] items;

    public UndoOperationCompletedEvent(final EventSource source, final Workspace workspace, final ItemSpec[] items) {
        super(source, workspace, ProcessType.UNDO);

        Check.notNull(items, "items"); //$NON-NLS-1$

        this.items = items;
    }

    /**
     * @return the items which were undone.
     */
    public ItemSpec[] getItems() {
        return items;
    }
}
