// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.conflicts.resolutions.contributors;

import java.util.ArrayList;
import java.util.Collection;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.conflicts.resolutions.EclipseMergeConflictResolution;
import com.microsoft.tfs.core.clients.versioncontrol.PropertiesMergeSummary;
import com.microsoft.tfs.core.clients.versioncontrol.PropertyConstants;
import com.microsoft.tfs.core.clients.versioncontrol.PropertyUtils;
import com.microsoft.tfs.core.clients.versioncontrol.conflicts.ConflictDescription;
import com.microsoft.tfs.core.clients.versioncontrol.conflicts.resolutions.ConflictResolution;
import com.microsoft.tfs.core.clients.versioncontrol.conflicts.resolutions.ConflictResolutionOptions;
import com.microsoft.tfs.core.clients.versioncontrol.conflicts.resolutions.contributors.ConflictResolutionContributor;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ChangeType;
import com.microsoft.tfs.jni.FileSystemUtils;

public final class EclipseMergeConflictResolutionContributor implements ConflictResolutionContributor {
    public EclipseMergeConflictResolutionContributor() {
    }

    @Override
    public Collection<ConflictResolution> getConflictResolutions(
        final ConflictDescription conflictDescription,
        final ConflictResolutionOptions resolutionOptions) {
        if (conflictDescription == null || conflictDescription.getConflict() == null) {
            return null;
        }

        final ChangeType deleteAtRollback = ChangeType.ROLLBACK.combine(ChangeType.DELETE);
        if (conflictDescription.getConflict().getBaseChangeType().contains(deleteAtRollback)) {
            return null;
        }

        /*
         * disable Merge Tool for symlinks
         */
        final PropertiesMergeSummary summary = conflictDescription.getConflict().getPropertiesMergeSummary();
        if (summary != null
            && summary.getMergedProperties() != null
            && PropertyConstants.IS_SYMLINK.equals(
                PropertyUtils.selectMatching(summary.getMergedProperties(), PropertyConstants.SYMBOLIC_KEY))) {
            return null;
        }

        final String localItem = conflictDescription.getConflict().getTargetLocalItem();
        if (localItem != null && FileSystemUtils.getInstance().getAttributes(localItem).isSymbolicLink()) {
            return null;
        }

        final Collection<ConflictResolution> resolutions = new ArrayList<ConflictResolution>();

        resolutions.add(
            new EclipseMergeConflictResolution(
                conflictDescription,
                Messages.getString("EclipseMergeConflictResolutionContributor.ResolutionName"), //$NON-NLS-1$
                Messages.getString("EclipseMergeConflictResolutionContributor.ResolutionHelpText"), //$NON-NLS-1$
                resolutionOptions));

        return resolutions;
    }
}
