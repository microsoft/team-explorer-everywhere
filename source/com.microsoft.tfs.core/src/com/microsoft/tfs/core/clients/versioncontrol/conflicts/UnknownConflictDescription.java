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
public class UnknownConflictDescription extends ConflictDescription {
    protected UnknownConflictDescription(
        final Workspace workspace,
        final Conflict conflict,
        final ItemSpec[] conflictItemSpecs) {
        super(workspace, conflict, conflictItemSpecs);
    }

    @Override
    public ConflictCategory getConflictCategory() {
        return ConflictCategory.UNKNOWN;
    }

    @Override
    public String getName() {
        return Messages.getString("UnknownConflictDescription.Name"); //$NON-NLS-1$
    }

    @Override
    public String getDescription() {
        return Messages.getString("UnknownConflictDescription.Description"); //$NON-NLS-1$
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
