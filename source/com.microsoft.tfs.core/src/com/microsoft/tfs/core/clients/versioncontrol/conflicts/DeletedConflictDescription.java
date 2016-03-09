// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.conflicts;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.core.clients.versioncontrol.conflicts.resolutions.ConflictResolution;
import com.microsoft.tfs.core.clients.versioncontrol.conflicts.resolutions.CoreConflictResolution;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Conflict;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Resolution;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;

/**
 * @since TEE-SDK-10.1
 */
public abstract class DeletedConflictDescription extends VersionConflictDescription {
    protected DeletedConflictDescription(
        final Workspace workspace,
        final Conflict conflict,
        final ItemSpec[] conflictItemSpecs) {
        super(workspace, conflict, conflictItemSpecs);
    }

    /**
     * For deleted conflicts, the change summary cannot be determined (since the
     * target does not exist.)
     *
     * {@inheritDoc}
     */
    @Override
    public boolean showChangeDescription() {
        return false;
    }

    /**
     * Deleted conflicts do not have change descriptions. Since
     * showChangeDescription() always returns false, this method should never be
     * called.
     *
     * {@inheritDoc}
     */
    @Override
    public String getChangeDescription() {
        return Messages.getString("DeletedConflictDescription.ChangeDescription"); //$NON-NLS-1$
    }

    /**
     * Deleted conflicts do not analyze (they're missing a file critical in
     * analyzing the conflict.)
     *
     * {@inheritDoc}
     */
    @Override
    public boolean analyzeConflict() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isResolutionEnabled(final ConflictResolution resolution) {
        if (getConflict() == null) {
            return true;
        }

        /*
         * Visual Studio's UI *shows* all resolutions (automerge and external
         * merge) even though they're disabled. Mimic that behavior by only
         * enabling core conflict resolution options that aren't automerge.
         */
        return (resolution instanceof CoreConflictResolution
            && ((CoreConflictResolution) resolution).getResolution() != Resolution.ACCEPT_MERGE);
    }
}
