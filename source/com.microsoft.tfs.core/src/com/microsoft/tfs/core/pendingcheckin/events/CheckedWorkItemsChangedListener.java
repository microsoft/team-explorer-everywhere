// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.pendingcheckin.events;

import java.util.EventListener;

import com.microsoft.tfs.core.pendingcheckin.PendingCheckinWorkItems;

/**
 * <p>
 * Defines an interface for listeners of the
 * {@link CheckedWorkItemsChangedEvent}.
 * </p>
 *
 * @since TEE-SDK-10.1
 * @threadsafety thread-safe
 */
public interface CheckedWorkItemsChangedListener extends EventListener {
    /**
     * Invoked when the checked work items in the user interface changes in a
     * {@link PendingCheckinWorkItems} object.
     *
     * @param e
     *        the event that describes the changes.
     */
    public void onCheckedWorkItemsChangesChanged(CheckedWorkItemsChangedEvent e);
}
