// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.pendingcheckin;

import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;

/**
 * <p>
 * An interface to an object that contains changes (version control, work items,
 * checkin notes, etc.) that are about to be checked in.
 * </p>
 * <p>
 * A {@link PendingCheckin} is designed to be evaluated in a {@link Workspace}.
 * The objects returned by these methods must <b>not</b> be modified during
 * evaluation. They may be modified at other times, and must be thread-safe.
 * </p>
 *
 * @since TEE-SDK-10.1
 * @threadsafety thread-safe
 */
public interface PendingCheckin {
    /**
     * @return the checkin notes set for this checkin.
     */
    public PendingCheckinNotes getCheckinNotes();

    /**
     * @return the pending changes selected for this checkin.
     */
    public PendingCheckinPendingChanges getPendingChanges();

    /**
     * @return the checkin policies enabled for this checkin.
     */
    public PendingCheckinPolicies getCheckinPolicies();

    /**
     * @return the work items selected for modification for this checkin.
     */
    public PendingCheckinWorkItems getWorkItems();
}
