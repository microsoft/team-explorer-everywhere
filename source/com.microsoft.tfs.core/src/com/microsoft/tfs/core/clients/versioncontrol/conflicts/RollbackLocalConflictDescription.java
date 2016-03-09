// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.conflicts;

import java.text.MessageFormat;
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

public class RollbackLocalConflictDescription extends VersionConflictDescription {
    protected RollbackLocalConflictDescription(
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
        return ConflictCategory.ROLLBACK_LOCAL;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return Messages.getString("RollbackLocalConflictDescription.Name"); //$NON-NLS-1$
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return Messages.getString("RollbackLocalConflictDescription.Description"); //$NON-NLS-1$
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConflictResolution[] getResolutions(final ConflictResolutionContributor conflictResolutionContributor) {
        final List<ConflictResolution> resolutionList = new ArrayList<ConflictResolution>();

        final Conflict conflict = getConflict();

        ConflictResolutionOptions resolutionOptions = ConflictResolutionOptions.NONE;

        if (filesRenamed() && isEncodingChange()) {
            resolutionOptions =
                ConflictResolutionOptions.SELECT_NAME.combine(ConflictResolutionOptions.SELECT_ENCODING);

            resolutionList.add(
                new CoreConflictResolution(
                    this,
                    ConflictDescriptionStrings.RENAME_ENCODING_AND_AUTOMERGE,
                    ConflictDescriptionStrings.RENAME_ENCODING_AND_AUTOMERGE_TOOLTIP,
                    resolutionOptions,
                    Resolution.ACCEPT_MERGE));
        } else if (filesRenamedOnly()) {
            resolutionList.add(new CoreConflictResolution(
                this,
                ConflictDescriptionStrings.RENAME,
                ConflictDescriptionStrings.RENAME_TOOLTIP,
                ConflictResolutionOptions.SELECT_NAME,
                Resolution.ACCEPT_MERGE));
        } else if (filesRenamed()) {
            resolutionList.add(new CoreConflictResolution(
                this,
                ConflictDescriptionStrings.RENAME_AND_AUTOMERGE,
                ConflictDescriptionStrings.RENAME_AND_AUTOMERGE_TOOLTIP,
                ConflictResolutionOptions.SELECT_NAME,
                Resolution.ACCEPT_MERGE));

            resolutionOptions = ConflictResolutionOptions.SELECT_NAME;
        } else if (isEncodingChange()) {
            resolutionList.add(new CoreConflictResolution(
                this,
                MessageFormat.format(
                    ConflictDescriptionStrings.SELECT_ENCODING_AND_AUTOMERGE,
                    getLocalFileDescription(),
                    getRemoteFileDescription()),
                ConflictDescriptionStrings.SELECT_ENCODING_AND_AUTOMERGE_TOOLTIP,
                ConflictResolutionOptions.SELECT_ENCODING,
                Resolution.ACCEPT_MERGE));

            resolutionOptions = ConflictResolutionOptions.SELECT_ENCODING;
        } else if (conflict != null
            && !conflict.getBaseChangeType().containsAll(ChangeType.ROLLBACK.combine(ChangeType.DELETE))) {
            resolutionList.add(
                new CoreConflictResolution(
                    this,
                    ConflictDescriptionStrings.AUTOMERGE,
                    MessageFormat.format(
                        ConflictDescriptionStrings.AUTOMERGE_TOOLTIP,
                        getLocalFileDescription(),
                        getRemoteFileDescription()),
                    ConflictResolutionOptions.NONE,
                    Resolution.ACCEPT_MERGE));
        }

        /*
         * We do not allow contributed resolutions in rename-only (non-edit)
         * conflicts. Any modification of the local file will result in a
         * writable conflict.
         */
        if (!filesRenamedOnly() && conflictResolutionContributor != null) {
            resolutionList.addAll(loadContributedResolutions(conflictResolutionContributor, resolutionOptions));
        }

        resolutionList.add(new CoreConflictResolution(
            this,
            ConflictDescriptionStrings.ROLLBACK_LOCAL_ACCEPT_YOURS,
            ConflictDescriptionStrings.ROLLBACK_LOCAL_ACCEPT_YOURS_TOOLTIP,
            ConflictResolutionOptions.NONE,
            Resolution.ACCEPT_YOURS));

        resolutionList.add(
            new CoreConflictResolution(
                this,
                ConflictDescriptionStrings.ROLLBACK_LOCAL_ACCEPT_THEIRS,
                ConflictDescriptionStrings.ROLLBACK_LOCAL_ACCEPT_THEIRS_TOOLTIP,
                ConflictResolutionOptions.NONE,
                Resolution.ACCEPT_THEIRS));

        return resolutionList.toArray(new ConflictResolution[resolutionList.size()]);
    }
}
