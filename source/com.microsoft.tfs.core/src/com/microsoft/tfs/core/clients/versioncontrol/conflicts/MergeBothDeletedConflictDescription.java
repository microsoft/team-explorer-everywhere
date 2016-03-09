// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.conflicts;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.core.clients.versioncontrol.conflicts.resolutions.ConflictResolution;
import com.microsoft.tfs.core.clients.versioncontrol.conflicts.resolutions.ConflictResolutionOptions;
import com.microsoft.tfs.core.clients.versioncontrol.conflicts.resolutions.CoreConflictResolution;
import com.microsoft.tfs.core.clients.versioncontrol.conflicts.resolutions.contributors.ConflictResolutionContributor;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Conflict;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Resolution;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;

/**
 * This is a deleted conflict, occurring when you attempt to delete a file that
 * the server has already removed.
 *
 * Note that this is unused in TFS 2010.
 *
 * @since TEE-SDK-10.1
 */
public class MergeBothDeletedConflictDescription extends DeletedConflictDescription {
    protected MergeBothDeletedConflictDescription(
        final Workspace workspace,
        final Conflict conflict,
        final ItemSpec[] conflictItemSpecs) {
        super(workspace, conflict, conflictItemSpecs);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return Messages.getString("MergeBothDeletedConflictDescription.Name"); //$NON-NLS-1$
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConflictCategory getConflictCategory() {
        return ConflictCategory.MERGE_BOTH_DELETED;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return Messages.getString("MergeBothDeletedConflictDescription.Description"); //$NON-NLS-1$
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConflictResolution[] getResolutions(final ConflictResolutionContributor resolutionContributor) {
        return new ConflictResolution[] {
            new CoreConflictResolution(
                this,
                Messages.getString("MergeBothDeletedConflictDescription.DiscardChangesFromSourceBranch"), //$NON-NLS-1$
                //@formatter:off
                Messages.getString("MergeBothDeletedConflictDescription.OptionWillContinueWithoutErrorBothItemsDeleted"), //$NON-NLS-1$
                //@formatter:on
                ConflictResolutionOptions.NONE,
                Resolution.ACCEPT_YOURS)
        };
    }
}