// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.conflicts.resolutions;

/**
 * @since TEE-SDK-10.1
 */
public interface ConflictResolutionStatusListener {
    /**
     * Callback for ConflictResolution to update the progress of resolution.
     *
     * @param conflictResolution
     *        The ConflictResolution object sending this message.
     * @param newStatus
     *        The updated status for this resolution
     */
    public void statusChanged(final ConflictResolution conflictResolution, final ConflictResolutionStatus newStatus);
}
