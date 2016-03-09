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
 * the server has edited.
 *
 * @since TEE-SDK-10.1
 */
public class LocallyDeletedConflictDescription extends DeletedConflictDescription {
    protected LocallyDeletedConflictDescription(
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
        return ConflictCategory.LOCALLY_DELETED;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return Messages.getString("LocallyDeletedConflictDescription.Description"); //$NON-NLS-1$
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConflictResolution[] getResolutions(final ConflictResolutionContributor resolutionContributor) {
        return new ConflictResolution[] {
            new CoreConflictResolution(
                this,
                ConflictDescriptionStrings.VERSION_ACCEPT_THEIRS,
                ConflictDescriptionStrings.VERSION_ACCEPT_THEIRS_TOOLTIP,
                ConflictResolutionOptions.NONE,
                Resolution.ACCEPT_THEIRS),
            new CoreConflictResolution(
                this,
                ConflictDescriptionStrings.VERSION_ACCEPT_YOURS,
                ConflictDescriptionStrings.VERSION_ACCEPT_YOURS_TOOLTIP,
                ConflictResolutionOptions.NONE,
                Resolution.ACCEPT_YOURS)
        };
    }
}