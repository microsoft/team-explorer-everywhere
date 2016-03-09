// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.conflicts;

import java.util.ArrayList;
import java.util.List;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.core.clients.versioncontrol.conflicts.resolutions.ConflictResolution;
import com.microsoft.tfs.core.clients.versioncontrol.conflicts.resolutions.ConflictResolutionOptions;
import com.microsoft.tfs.core.clients.versioncontrol.conflicts.resolutions.CoreConflictResolution;
import com.microsoft.tfs.core.clients.versioncontrol.conflicts.resolutions.contributors.ConflictResolutionContributor;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ChangeType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Conflict;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Resolution;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;

/**
 * This is a deleted conflict, occurring when you attempt to edit a file such
 * that the remote file has been deleted.
 *
 * @since TEE-SDK-10.1
 */
public class ServerDeletedConflictDescription extends DeletedConflictDescription {
    protected ServerDeletedConflictDescription(
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
        return ConflictCategory.SERVER_DELETED;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        final Conflict conflict = getConflict();

        /* Dev10: could be a remote rename. */
        if (conflict != null && conflict.getTheirChangeType().contains(ChangeType.SOURCE_RENAME)) {
            return Messages.getString("ServerDeletedConflictDescription.DescriptionRenamedOnServer"); //$NON-NLS-1$
        }

        return Messages.getString("ServerDeletedConflictDescription.DescriptionAlreadyDeleted"); //$NON-NLS-1$
    };

    /**
     * {@inheritDoc}
     */
    @Override
    public ConflictResolution[] getResolutions(final ConflictResolutionContributor resolutionContributor) {
        final List<ConflictResolution> resolutionList = new ArrayList<ConflictResolution>();

        resolutionList.add(
            new CoreConflictResolution(
                this,
                ConflictDescriptionStrings.VERSION_ACCEPT_THEIRS,
                ConflictDescriptionStrings.VERSION_ACCEPT_THEIRS_TOOLTIP,
                ConflictResolutionOptions.NONE,
                Resolution.ACCEPT_THEIRS));

        resolutionList.add(
            new CoreConflictResolution(
                this,
                ConflictDescriptionStrings.VERSION_DELETED_ACCEPT_YOURS,
                ConflictDescriptionStrings.VERSION_DELETED_ACCEPT_YOURS_TOOLTIP,
                ConflictResolutionOptions.NONE,
                Resolution.ACCEPT_YOURS));

        return resolutionList.toArray(new ConflictResolution[resolutionList.size()]);
    }
}
