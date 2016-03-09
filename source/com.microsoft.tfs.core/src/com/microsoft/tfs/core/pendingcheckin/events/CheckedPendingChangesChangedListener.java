// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.pendingcheckin.events;

import java.util.EventListener;

import com.microsoft.tfs.core.pendingcheckin.PendingCheckinPendingChanges;

/**
 * <p>
 * Defines an interface for listeners of the
 * {@link CheckedPendingChangesChangedEvent}.
 * </p>
 *
 * @since TEE-SDK-10.1
 * @threadsafety thread-safe
 */
public interface CheckedPendingChangesChangedListener extends EventListener {
    /**
     * Invoked when the checked pending changes in the user interface changes in
     * a {@link PendingCheckinPendingChanges} object.
     *
     * @param e
     *        the event that describes the changes.
     */
    public void onCheckedPendingChangesChanged(CheckedPendingChangesChangedEvent e);
}
