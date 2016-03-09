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
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Conflict;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Resolution;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;

/**
 * This is a merge deleted conflict, occurring when you attempt to merge such
 * that a file in the source branch has been modified and the file in the target
 * branch has been deleted. It is a special case of
 * {@link DeletedConflictDescription}.
 *
 * @since TEE-SDK-10.1
 */
public final class MergeTargetDeletedConflictDescription extends DeletedConflictDescription {
    protected MergeTargetDeletedConflictDescription(
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
        return Messages.getString("MergeTargetDeletedConflictDescription.Name"); //$NON-NLS-1$
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConflictCategory getConflictCategory() {
        return ConflictCategory.MERGE_TARGET_DELETED;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return Messages.getString("MergeTargetDeletedConflictDescription.Description"); //$NON-NLS-1$
    }

    /**
     * The local file in a merge conflict is called the "source".
     *
     * {@inheritDoc}
     */
    @Override
    public String getLocalFileDescription() {
        return Messages.getString("MergeTargetDeletedConflictDescription.LocalFileDescription"); //$NON-NLS-1$
    }

    /**
     * The server file in a merge conflict is called the "target".
     *
     * {@inheritDoc}
     */
    @Override
    public String getRemoteFileDescription() {
        return Messages.getString("MergeTargetDeletedConflictDescription.ServerFileDescription"); //$NON-NLS-1$
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConflictResolution[] getResolutions(final ConflictResolutionContributor resolutionContributor) {
        final List<ConflictResolution> resolutionList = new ArrayList<ConflictResolution>();

        resolutionList.add(
            new CoreConflictResolution(
                this,
                ConflictDescriptionStrings.MERGE_TARGET_DELETED_AUTOMERGE,
                ConflictDescriptionStrings.MERGE_TARGET_DELETED_AUTOMERGE_TOOLTIP,
                ConflictResolutionOptions.NONE,
                Resolution.ACCEPT_MERGE));

        resolutionList.addAll(loadContributedResolutions(resolutionContributor, ConflictResolutionOptions.NONE));

        resolutionList.add(new CoreConflictResolution(
            this,
            ConflictDescriptionStrings.MERGE_ACCEPT_YOURS,
            ConflictDescriptionStrings.MERGE_ACCEPT_YOURS_TOOLTIP,
            ConflictResolutionOptions.NONE,
            Resolution.ACCEPT_YOURS));

        resolutionList.add(
            new CoreConflictResolution(
                this,
                ConflictDescriptionStrings.MERGE_ACCEPT_THEIRS,
                ConflictDescriptionStrings.MERGE_ACCEPT_THEIRS_TOOLTIP,
                ConflictResolutionOptions.NONE,
                Resolution.ACCEPT_THEIRS));

        return resolutionList.toArray(new ConflictResolution[resolutionList.size()]);
    }
}