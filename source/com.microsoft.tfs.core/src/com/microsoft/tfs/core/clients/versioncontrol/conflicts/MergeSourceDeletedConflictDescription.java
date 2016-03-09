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
 * @since TEE-SDK-10.1
 */
public class MergeSourceDeletedConflictDescription extends DeletedConflictDescription {
    protected MergeSourceDeletedConflictDescription(
        final Workspace workspace,
        final Conflict conflict,
        final ItemSpec[] conflictItemSpecs) {
        super(workspace, conflict, conflictItemSpecs);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConflictCategory getConflictCategory() {
        return ConflictCategory.MERGE_SOURCE_DELETED;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return Messages.getString("MergeSourceDeletedConflictDescription.Name"); //$NON-NLS-1$
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return Messages.getString("MergeSourceDeletedConflictDescription.Description"); //$NON-NLS-1$
    }

    /**
     * The local file in a merge conflict is called the "source".
     *
     * {@inheritDoc}
     */
    @Override
    public String getLocalFileDescription() {
        return Messages.getString("MergeSourceDeletedConflictDescription.LocalFileDescription"); //$NON-NLS-1$
    }

    /**
     * The server file in a merge conflict is called the "target".
     *
     * {@inheritDoc}
     */
    @Override
    public String getRemoteFileDescription() {
        return Messages.getString("MergeSourceDeletedConflictDescription.ServerFileDescription"); //$NON-NLS-1$
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConflictResolution[] getResolutions(final ConflictResolutionContributor resolutionContributor) {
        return new ConflictResolution[] {
            new CoreConflictResolution(
                this,
                ConflictDescriptionStrings.MERGE_ACCEPT_YOURS,
                ConflictDescriptionStrings.MERGE_ACCEPT_YOURS_TOOLTIP,
                ConflictResolutionOptions.NONE,
                Resolution.ACCEPT_YOURS),

            new CoreConflictResolution(
                this,
                ConflictDescriptionStrings.MERGE_ACCEPT_THEIRS,
                ConflictDescriptionStrings.MERGE_ACCEPT_THEIRS_TOOLTIP,
                ConflictResolutionOptions.NONE,
                Resolution.ACCEPT_THEIRS)
        };
    }
}
