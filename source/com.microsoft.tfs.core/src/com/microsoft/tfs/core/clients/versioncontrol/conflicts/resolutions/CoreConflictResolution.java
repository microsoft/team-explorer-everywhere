// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.conflicts.resolutions;

import java.text.MessageFormat;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.core.clients.versioncontrol.ResolutionOptions.EncodingStrategy;
import com.microsoft.tfs.core.clients.versioncontrol.conflicts.ConflictDescription;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Conflict;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Resolution;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.core.exceptions.TECoreException;
import com.microsoft.tfs.core.util.FileEncoding;
import com.microsoft.tfs.util.Check;

/**
 * A CoreConflictResolution is a type of ConflictResolution which knows how to
 * resolve conflicts using the core conflict resolution mechanism. (
 * {@link #resolveConflict()}). This is used for most conflicts.
 *
 * @since TEE-SDK-10.1
 */
public class CoreConflictResolution extends ConflictResolution {
    private final Resolution resolution;
    private String newPath;
    private FileEncoding newEncoding;

    public CoreConflictResolution(
        final ConflictDescription conflictDescription,
        final String description,
        final String helpText,
        final ConflictResolutionOptions options,
        final Resolution resolution) {
        super(conflictDescription, description, helpText, options);

        Check.notNull(resolution, "resolution"); //$NON-NLS-1$

        this.resolution = resolution;
    }

    @Override
    public ConflictResolution newForConflictDescription(final ConflictDescription conflictDescription) {
        return new CoreConflictResolution(
            conflictDescription,
            getDescription(),
            getHelpText(),
            ConflictResolutionOptions.NONE,
            resolution);
    }

    @Override
    public void setNewPath(final String newPath) {
        if (!needsNewPath()) {
            throw new TECoreException(MessageFormat.format(
                Messages.getString("CoreConflictResolution.ResolutionTypeDoesNotAcceptNewPathsFormat"), //$NON-NLS-1$
                getDescription()));
        }

        this.newPath = newPath;
    }

    @Override
    public void setEncoding(final FileEncoding newEncoding) {
        if (!needsEncodingSelection()) {
            throw new TECoreException(MessageFormat.format(
                Messages.getString("CoreConflictResolution.ResolutionTypeDoesNotAcceptNewEncodingsFormat"), //$NON-NLS-1$
                getDescription()));
        }

        this.newEncoding = newEncoding;
    }

    @Override
    public ConflictResolutionStatus work() {
        final ConflictDescription conflictDescription = getConflictDescription();

        final Workspace workspace = conflictDescription.getWorkspace();
        final Conflict conflict = conflictDescription.getConflict();

        // set the resolution type, attempt to have core resolve it
        conflict.setResolution(resolution);

        // set the new path if necessary
        if (newPath != null) {
            conflict.getResolutionOptions().setNewPath(newPath);
        }

        // set new encoding if necessary
        if (newEncoding != null) {
            /*
             * The encoding strategy is strictly for the merge engine - it
             * determines the output file's encoding. The acceptMergeEncoding
             * option is strictly for the resolveConflict web services call - it
             * determines the encoding type metadata used by tfs. Both must be
             * specified.
             */
            conflict.getResolutionOptions().setEncodingStrategy(EncodingStrategy.CONVERT_EXPLICIT, newEncoding);
            conflict.getResolutionOptions().setAcceptMergeEncoding(newEncoding);
        }

        workspace.resolveConflict(conflict);

        // ensure that the conflict was actually resolved. this may not be
        // resolved, particularly in the case of automerge
        if (conflict.isResolved()) {
            return ConflictResolutionStatus.SUCCESS;
        } else {
            /* Get error messages from merge failures */
            if (conflict.getContentMergeSummary() != null
                && conflict.getContentMergeSummary().getTotalConflictingLines() > 0) {
                setErrorMessage(Messages.getString("CoreConflictResolution.ConflictingContentChanges")); //$NON-NLS-1$
            }

            conflict.setResolution(Resolution.NONE);
            return ConflictResolutionStatus.FAILED;
        }
    }

    public Resolution getResolution() {
        return resolution;
    }
}
