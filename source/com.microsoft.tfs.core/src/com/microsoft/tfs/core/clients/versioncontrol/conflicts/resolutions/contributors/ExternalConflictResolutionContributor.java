// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.conflicts.resolutions.contributors;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.core.clients.versioncontrol.conflicts.ConflictDescription;
import com.microsoft.tfs.core.clients.versioncontrol.conflicts.resolutions.ConflictResolution;
import com.microsoft.tfs.core.clients.versioncontrol.conflicts.resolutions.ConflictResolutionOptions;
import com.microsoft.tfs.core.clients.versioncontrol.conflicts.resolutions.ExternalConflictResolution;
import com.microsoft.tfs.core.clients.versioncontrol.path.LocalPath;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ChangeType;
import com.microsoft.tfs.core.externaltools.ExternalTool;
import com.microsoft.tfs.core.externaltools.ExternalToolset;

/**
 * @since TEE-SDK-10.1
 */
public final class ExternalConflictResolutionContributor implements ConflictResolutionContributor {
    private final ExternalToolset mergeToolset;

    public ExternalConflictResolutionContributor(final ExternalToolset mergeToolset) {
        this.mergeToolset = mergeToolset;
    }

    @Override
    public Collection<ConflictResolution> getConflictResolutions(
        final ConflictDescription conflictDescription,
        final ConflictResolutionOptions resolutionOptions) {
        /*
         * Note: conflict may be null here - MultipleConflictResolutionControl
         * sets up fake conflict descriptions (ie, they lack a Conflict object)
         * to determine appropriate resolution options. External merge should
         * never be appropriate in this case.
         */
        if (conflictDescription == null || conflictDescription.getConflict() == null || mergeToolset == null) {
            return null;
        }

        final ChangeType deleteAtRollback = ChangeType.ROLLBACK.combine(ChangeType.DELETE);
        if (conflictDescription.getConflict().getBaseChangeType().containsAll(deleteAtRollback)) {
            return null;
        }

        final ExternalTool mergeTool = mergeToolset.findTool(conflictDescription.getLocalPath());

        if (mergeTool == null) {
            return null;
        }

        final String programName = LocalPath.getFileName(mergeTool.getOriginalCommand());
        final String resolutionDescriptionText =
            MessageFormat.format(
                Messages.getString("ExternalConflictResolutionContributor.ResolutionNameFormat"), //$NON-NLS-1$
                programName);
        final String resolutionHelpText =
            MessageFormat.format(
                Messages.getString("ExternalConflictResolutionContributor.ResolutionHelpTextFormat"), //$NON-NLS-1$
                programName);

        final Collection<ConflictResolution> resolutions = new ArrayList<ConflictResolution>();

        resolutions.add(
            new ExternalConflictResolution(
                conflictDescription,
                resolutionDescriptionText,
                resolutionHelpText,
                resolutionOptions,
                mergeToolset));

        return resolutions;
    }
}
