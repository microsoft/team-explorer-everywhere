// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.conflicts;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.core.clients.versioncontrol.WebServiceLevel;
import com.microsoft.tfs.core.clients.versioncontrol.conflicts.resolutions.ConflictResolution;
import com.microsoft.tfs.core.clients.versioncontrol.conflicts.resolutions.ConflictResolutionOptions;
import com.microsoft.tfs.core.clients.versioncontrol.conflicts.resolutions.CoreConflictResolution;
import com.microsoft.tfs.core.clients.versioncontrol.conflicts.resolutions.contributors.ConflictResolutionContributor;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Conflict;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Resolution;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;

/**
 * This is a filename conflict, occurring when a get request would produce a
 * local file with the same name as an already existing file.
 *
 * @since TEE-SDK-10.1
 */
public final class FilenameConflictDescription extends ConflictDescription {
    protected FilenameConflictDescription(
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
        return ConflictCategory.FILENAME;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return Messages.getString("FilenameConflictDescription.Name"); //$NON-NLS-1$
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return Messages.getString("FilenameConflictDescription.Description"); //$NON-NLS-1$
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConflictResolution[] getResolutions(final ConflictResolutionContributor resolutionContributor) {
        final Workspace workspace = getWorkspace();

        /*
         * 2010 only allows accept theirs / accept yours, these are appropriate
         * for multi-select too
         */
        if (workspace == null
            || workspace.getClient().getServiceLevel().getValue() >= WebServiceLevel.TFS_2010.getValue()) {
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
        /* VS 2008 offers rename theirs / rename yours options */
        else {
            return new ConflictResolution[] {
                /*
                 * Rename yours is commented out: we do not quite handle the
                 * resultant getops properly.
                 */
                new CoreConflictResolution(
                    this,
                    ConflictDescriptionStrings.VERSION_RENAME_LOCAL,
                    ConflictDescriptionStrings.VERSION_RENAME_LOCAL_TOOLTIP,
                    ConflictResolutionOptions.SELECT_NAME,
                    Resolution.ACCEPT_MERGE),
                new CoreConflictResolution(
                    this,
                    ConflictDescriptionStrings.VERSION_RENAME_SERVER,
                    ConflictDescriptionStrings.VERSION_RENAME_SERVER_TOOLTIP,
                    ConflictResolutionOptions.SELECT_NAME,
                    Resolution.ACCEPT_YOURS_RENAME_THEIRS),
                new CoreConflictResolution(
                    this,
                    ConflictDescriptionStrings.VERSION_ACCEPT_THEIRS,
                    ConflictDescriptionStrings.VERSION_ACCEPT_THEIRS_TOOLTIP,
                    ConflictResolutionOptions.NONE,
                    Resolution.ACCEPT_THEIRS)
            };
        }
    }
}