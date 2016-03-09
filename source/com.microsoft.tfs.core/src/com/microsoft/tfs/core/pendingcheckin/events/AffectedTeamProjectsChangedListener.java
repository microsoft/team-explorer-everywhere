// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.pendingcheckin.events;

import java.util.EventListener;

import com.microsoft.tfs.core.pendingcheckin.PendingCheckinPendingChanges;

/**
 * <p>
 * Defines an interface for listeners of the
 * {@link AffectedTeamProjectsChangedEvent}.
 * </p>
 *
 * @since TEE-SDK-10.1
 * @threadsafety thread-safe
 */
public interface AffectedTeamProjectsChangedListener extends EventListener {
    /**
     * Invoked when the collection of affected team projects changes in a
     * {@link PendingCheckinPendingChanges} object.
     *
     * @param e
     *        the event that describes the changes.
     */
    public void onAffectedTeamProjectsChanged(AffectedTeamProjectsChangedEvent e);
}
