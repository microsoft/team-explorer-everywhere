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
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ChangeType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Conflict;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Resolution;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;

/**
 * This is a merge conflict, occurring when you attempt to merge two branches,
 * and a file has been changed in both branches.
 *
 * Note that in Visual Studio UI parlance, "accept theirs" means to accept the
 * merge source; "accept yours" means to accept the merge target.
 *
 * Updated for TFS 2010.
 *
 * @since TEE-SDK-10.1
 */
public class MergeConflictDescription extends VersionConflictDescription {
    protected MergeConflictDescription(
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
        return ConflictCategory.MERGE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return Messages.getString("MergeConflictDescription.Name"); //$NON-NLS-1$
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        final Conflict conflict = getConflict();

        if (filesRenamed()
            && conflict != null
            && (conflict.getBaseItemType() == ItemType.FOLDER
                && conflict.getTheirItemType() == ItemType.FOLDER
                && conflict.getTheirItemType() == ItemType.FOLDER)) {
            return Messages.getString("MergeConflictDescription.DescriptionChanged"); //$NON-NLS-1$
        } else if (isBaseless()) {
            return Messages.getString("MergeConflictDescription.BaselessMerge"); //$NON-NLS-1$
        } else if (filesRenamedOnly()) {
            return Messages.getString("MergeConflictDescription.DescriptionRenamedOnly"); //$NON-NLS-1$
        } else if (filesRenamed() && isEncodingChange()) {
            return Messages.getString("MergeConflictDescription.DescriptionRenamedAndEncodingChange"); //$NON-NLS-1$
        } else if (filesRenamed()) {
            return Messages.getString("MergeConflictDescription.DescriptionRenamed"); //$NON-NLS-1$
        } else if (isEncodingChange()) {
            return Messages.getString("MergeConflictDescription.DescriptionEncodingChange"); //$NON-NLS-1$
        }

        return Messages.getString("MergeConflictDescription.DescriptionBothHaveChanges"); //$NON-NLS-1$
    }

    /**
     * The local file in a merge conflict is called the "source".
     *
     * {@inheritDoc}
     */
    @Override
    public String getLocalFileDescription() {
        return Messages.getString("MergeConflictDescription.LocalFileDescription"); //$NON-NLS-1$
    }

    /**
     * The server file in a merge conflict is called the "target".
     *
     * {@inheritDoc}
     */
    @Override
    public String getRemoteFileDescription() {
        return Messages.getString("MergeConflictDescription.ServerFileDescription"); //$NON-NLS-1$
    }

    @Override
    public String getServerPath() {
        return getConflict().getYourServerItemSource();
    }

    @Override
    protected boolean filesRenamed() {
        final Conflict conflict = getConflict();

        /*
         * Note: conflict may be null here - MultipleConflictResolutionControl
         * sets up fake conflict descriptions (ie, they lack a Conflict object)
         * to determine appropriate resolution options.
         */
        if (conflict != null) {
            return (conflict.getYourServerItemSource() != null
                && !ServerPath.equals(conflict.getYourServerItemSource(), conflict.getYourServerItem()));
        }

        return false;
    }

    @Override
    protected boolean targetRenamed() {
        final Conflict conflict = getConflict();

        /*
         * Note: conflict may be null here - MultipleConflictResolutionControl
         * sets up fake conflict descriptions (ie, they lack a Conflict object)
         * to determine appropriate resolution options.
         */
        if (conflict != null) {
            return (conflict.getYourChangeType() == null || conflict.getYourChangeType().contains(ChangeType.RENAME));
        }

        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConflictResolution[] getResolutions(final ConflictResolutionContributor resolutionContributor) {
        final List<ConflictResolution> resolutionList = new ArrayList<ConflictResolution>();

        /*
         * If both files were only renamed, they'll only have to select a name
         */
        if (filesRenamedOnly()) {
            resolutionList.add(new CoreConflictResolution(
                this,
                ConflictDescriptionStrings.RENAME,
                ConflictDescriptionStrings.RENAME_TOOLTIP,
                ConflictResolutionOptions.SELECT_NAME,
                Resolution.ACCEPT_MERGE));
        }

        /*
         * Do not offer automerge for target renames, but do for source renames
         * (requiring them to select a result name)
         */
        else if (filesRenamed()) {
            /* Conflict is with folders */
            if ((getConflict().getBaseItemType() == ItemType.FOLDER
                && getConflict().getTheirItemType() == ItemType.FOLDER
                && getConflict().getTheirItemType() == ItemType.FOLDER)) {
                resolutionList.add(
                    new CoreConflictResolution(
                        this,
                        ConflictDescriptionStrings.RENAME,
                        ConflictDescriptionStrings.RENAME_TOOLTIP,
                        ConflictResolutionOptions.SELECT_NAME,
                        Resolution.ACCEPT_MERGE));
            }
            /* Conflict includes files */
            else {
                ConflictResolutionOptions resolutionOptions;

                if (isEncodingChange()) {
                    resolutionList.add(new CoreConflictResolution(
                        this,
                        ConflictDescriptionStrings.RENAME_ENCODING_AND_AUTOMERGE,
                        ConflictDescriptionStrings.RENAME_ENCODING_AND_AUTOMERGE_TOOLTIP,
                        ConflictResolutionOptions.SELECT_NAME.combine(ConflictResolutionOptions.SELECT_ENCODING),
                        Resolution.ACCEPT_MERGE));

                    resolutionOptions =
                        ConflictResolutionOptions.SELECT_NAME.combine(ConflictResolutionOptions.SELECT_ENCODING);
                } else {
                    resolutionList.add(
                        new CoreConflictResolution(
                            this,
                            ConflictDescriptionStrings.RENAME_AND_AUTOMERGE,
                            ConflictDescriptionStrings.RENAME_AND_AUTOMERGE_TOOLTIP,
                            ConflictResolutionOptions.SELECT_NAME,
                            Resolution.ACCEPT_MERGE));

                    resolutionOptions = ConflictResolutionOptions.SELECT_NAME;
                }

                resolutionList.addAll(loadContributedResolutions(resolutionContributor, resolutionOptions));
            }
        } else if (!targetRenamed()) {
            ConflictResolutionOptions resolutionOptions;

            if (isEncodingChange()) {
                resolutionList.add(new CoreConflictResolution(
                    this,
                    ConflictDescriptionStrings.SELECT_ENCODING_AND_AUTOMERGE,
                    ConflictDescriptionStrings.SELECT_ENCODING_AND_AUTOMERGE_TOOLTIP,
                    ConflictResolutionOptions.SELECT_ENCODING,
                    Resolution.ACCEPT_MERGE));

                resolutionOptions = ConflictResolutionOptions.SELECT_ENCODING;
            } else {
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

                resolutionOptions = ConflictResolutionOptions.NONE;
            }

            if (resolutionContributor != null) {
                resolutionList.addAll(loadContributedResolutions(resolutionContributor, resolutionOptions));
            }
        }

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
