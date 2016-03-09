// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.pendingcheckin.events;

import com.microsoft.tfs.core.clients.CoreClientEvent;
import com.microsoft.tfs.core.clients.versioncontrol.events.EventSource;
import com.microsoft.tfs.core.pendingcheckin.PendingCheckinPendingChanges;

/**
 * <p>
 * Event fired when the pending changes that are "checked" in the user interface
 * changes in a {@link PendingCheckinPendingChanges} object.
 * </p>
 *
 * @since TEE-SDK-10.1
 * @threadsafety immutable
 */
public class CheckedPendingChangesChangedEvent extends CoreClientEvent {
    /**
     * Describes a change in checked pending changes.
     *
     */
    public CheckedPendingChangesChangedEvent(final EventSource source) {
        super(source);
    }
}
