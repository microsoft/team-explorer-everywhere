// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.conflicts;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.core.clients.versioncontrol.MergeSummary;
import com.microsoft.tfs.core.clients.versioncontrol.conflicts.resolutions.ConflictResolution;
import com.microsoft.tfs.core.clients.versioncontrol.conflicts.resolutions.ConflictResolutionOptions;
import com.microsoft.tfs.core.clients.versioncontrol.conflicts.resolutions.CoreConflictResolution;
import com.microsoft.tfs.core.clients.versioncontrol.conflicts.resolutions.contributors.ConflictResolutionContributor;
import com.microsoft.tfs.core.clients.versioncontrol.engines.MergeEngine;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ChangeType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Conflict;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Resolution;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;
import com.microsoft.tfs.core.util.FileEncoding;
import com.microsoft.tfs.util.Check;

/**
 * This is a version conflict - the typical type of conflict, occuring when
 * there have been both local and server changes to a file.
 *
 * @since TEE-SDK-10.1
 */
public class VersionConflictDescription extends ConflictDescription {
    private boolean hasAnalyzed = false;

    protected VersionConflictDescription(
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
        return ConflictCategory.VERSION;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return Messages.getString("VersionConflictDescription.Name"); //$NON-NLS-1$
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        if (filesRenamed() && isEncodingChange()) {
            return Messages.getString("VersionConflictDescription.DescriptionRenamedAndEncodingChange"); //$NON-NLS-1$
        }
        if (filesRenamedOnly()) {
            return Messages.getString("VersionConflictDescription.DescriptionRenamedOnly"); //$NON-NLS-1$
        }
        if (filesRenamed()) {
            return Messages.getString("VersionConflictDescription.DescriptionRenamed"); //$NON-NLS-1$
        }
        if (isEncodingChange()) {
            return Messages.getString("VersionConflictDescription.EncodingChanged"); //$NON-NLS-1$
        }

        return Messages.getString("VersionConflictDescription.DescriptionChanged"); //$NON-NLS-1$
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean showChangeDescription() {
        return true;
    }

    /**
     * Analyze the conflict for mergeability, etc. Running
     * mergeEngine.countContentConflicts() will populate the conflict object's
     * content conflict information.
     *
     * {@inheritDoc}
     */
    @Override
    public boolean analyzeConflict() {
        /*
         * Note: only called when resolving a single conflict - this is safe to
         * assume the Conflict object exists.
         */
        if (getConflict().getBaseItemType() == ItemType.FILE
            || getConflict().getYourItemType() == ItemType.FILE
            || getConflict().getTheirItemType() == ItemType.FILE) {
            // TODO: this typically runs in a seperate thread on the UI.
            // we should consider if we can lock AConflict's internal members.
            final Workspace workspace = getWorkspace();

            final MergeEngine mergeEngine = new MergeEngine(workspace, workspace.getClient());
            mergeEngine.countContentConflicts(getConflict());
        }

        hasAnalyzed = true;

        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasAnalyzed() {
        return hasAnalyzed;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clearAnalysis() {
        hasAnalyzed = false;
    }

    protected boolean filesRenamed() {
        final Conflict conflict = getConflict();

        /*
         * Note: conflict may be null here - MultipleConflictResolutionControl
         * sets up fake conflict descriptions (ie, they lack a Conflict object)
         * to determine appropriate resolution options.
         */
        if (conflict != null) {
            return (conflict.getBaseServerItem() == null
                || !conflict.getBaseServerItem().equals(conflict.getTheirServerItem()));
        }

        return false;
    }

    /*
     * Returns true if there were no edits pended in this conflict, only
     * renames.
     */
    protected boolean filesRenamedOnly() {
        final Conflict conflict = getConflict();

        /*
         * Note: conflict may be null here - MultipleConflictResolutionControl
         * sets up fake conflict descriptions (ie, they lack a Conflict object)
         * to determine appropriate resolution options.
         */
        if (conflict != null) {
            return (conflict.getYourChangeType().equals(ChangeType.RENAME)
                && conflict.getTheirChangeType().equals(ChangeType.RENAME));
        }

        return false;
    }

    private boolean filesBinary() {
        /*
         * Note: only called when resolving a single conflict - this is safe to
         * assume the Conflict object exists.
         */

        // TODO: query the server's file type for this file
        // (see ExamineMergeConflictCommand)
        final Conflict conflict = getConflict();

        return (conflict.getBaseEncoding() == FileEncoding.BINARY
            || conflict.getTheirEncoding() == FileEncoding.BINARY
            || conflict.getYourEncoding() == FileEncoding.BINARY);
    }

    protected boolean isEncodingChange() {
        final Conflict conflict = getConflict();

        /*
         * Note: conflict may be null here - MultipleConflictResolutionControl
         * sets up fake conflict descriptions (ie, they lack a Conflict object)
         * to determine appropriate resolution options.
         */
        if (conflict != null
            && !conflict.getBaseChangeType().containsAll(ChangeType.ROLLBACK.combine(ChangeType.DELETE))) {
            /*
             * If the two items encodings differ, this is an encoding conflict.
             */
            if (!conflict.getTheirEncoding().equals(conflict.getYourEncoding())) {
                return true;
            }

            /*
             * If the two items encodings are the same, but differ from the
             * base, this is an encoding conflict.
             */
            if (conflict.getBaseEncoding() != null
                && conflict.getBaseEncoding().getCodePage() > 0
                && !conflict.getTheirEncoding().equals(conflict.getBaseEncoding())) {
                return true;
            }
        }

        return false;
    }

    private boolean isAutomergeEnabled() {
        final Conflict conflict = getConflict();
        MergeSummary mergeSummary;

        // can't automerge if any file is binary
        if (filesBinary()) {
            return false;
        }

        // only rename change
        if (filesRenamedOnly()) {
            return true;
        } else if (isBaseless()) {
            // baseless merge
            return false;
        } else if (hasAnalyzed && (mergeSummary = conflict.getContentMergeSummary()) != null) {
            return (mergeSummary.getTotalConflictingLines() == 0);
        } else if (hasAnalyzed) {
            return false;
        }

        // unknown, let them try it
        return true;
    }

    /**
     * Exists only to be overridden by merge.
     *
     * @return true if the target was renamed
     */
    protected boolean targetRenamed() {
        return false;
    }

    /**
     * Gets the change description. Note that one should probably populate the
     * conflict ContentMergeSummary before calling this for it to be useful.
     * (See ConflictMergeSummaryCommand).
     *
     * {@inheritDoc}
     */
    @Override
    public String getChangeDescription() {
        String changeDescription;
        MergeSummary mergeSummary;
        final Conflict conflict = getConflict();

        // at least one file is binary
        if (filesBinary()) {
            changeDescription = ConflictDescriptionStrings.SUMMARY_BINARY;
        }

        else if (filesRenamedOnly()) {
            changeDescription = ConflictDescriptionStrings.SUMMARY_RENAMED;
        }

        // the ContentMergeSummary tells us all about lines changed
        else if (hasAnalyzed && (mergeSummary = conflict.getContentMergeSummary()) != null) {
            /* Content conflicts - ie, conflicting changed lines */
            if (mergeSummary.getTotalConflictingLines() > 0) {
                changeDescription = MessageFormat.format(
                    ConflictDescriptionStrings.SUMMARY_CONTENT_CONFLICT,
                    getLocalFileDescription(),
                    getRemoteFileDescription());
            }
            /* Content and encoding has changed */
            else if (isEncodingChange()) {
                changeDescription = ConflictDescriptionStrings.SUMMARY_CONTENT_AND_ENCODING_CHANGED;
            } else if (isBaseless()) {
                changeDescription = ConflictDescriptionStrings.BASELESS_MERGE_CONFLICT;
            }
            /* Both local and server edits */
            else {
                changeDescription = MessageFormat.format(
                    ConflictDescriptionStrings.SUMMARY_BOTH_CHANGED,
                    getLocalFileDescription(),
                    getRemoteFileDescription());
            }
        }

        else if (hasAnalyzed && targetRenamed()) {
            changeDescription = ConflictDescriptionStrings.SUMMARY_NO_MERGE_AVAILABLE;
        }

        // we've analyzed, but got no content merge summary
        else if (hasAnalyzed) {
            changeDescription = ConflictDescriptionStrings.SUMMARY_UNMERGEABLE;
        }

        // we've not analyzed, calling too soon
        else {
            changeDescription = Messages.getString("VersionConflictDescription.ChangeDescription"); //$NON-NLS-1$
        }

        return changeDescription;
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
            ConflictDescriptionStrings.VERSION_ACCEPT_THEIRS,
            ConflictDescriptionStrings.VERSION_ACCEPT_THEIRS_TOOLTIP,
            ConflictResolutionOptions.NONE,
            Resolution.ACCEPT_THEIRS));
        resolutionList.add(
            new CoreConflictResolution(
                this,
                ConflictDescriptionStrings.VERSION_ACCEPT_YOURS,
                ConflictDescriptionStrings.VERSION_ACCEPT_YOURS_TOOLTIP,
                ConflictResolutionOptions.NONE,
                Resolution.ACCEPT_YOURS));

        return resolutionList.toArray(new ConflictResolution[resolutionList.size()]);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isResolutionEnabled(final ConflictResolution resolution) {
        Check.notNull(resolution, "resolution"); //$NON-NLS-1$

        if (getConflict() == null) {
            return true;
        } else if ((resolution instanceof CoreConflictResolution)
            && ((CoreConflictResolution) resolution).getResolution() == Resolution.ACCEPT_MERGE) {
            /* Always enable ACCEPT_MERGE for all-folder conflicts. */
            if (getConflict().getBaseItemType() == ItemType.FOLDER
                && getConflict().getYourItemType() == ItemType.FOLDER
                && getConflict().getTheirItemType() == ItemType.FOLDER) {
                return true;
            }

            return isAutomergeEnabled();
        }

        return true;
    }
}
