// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.conflicts.resolutions.contributors;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.microsoft.tfs.core.clients.versioncontrol.conflicts.ConflictDescription;
import com.microsoft.tfs.core.clients.versioncontrol.conflicts.resolutions.ConflictResolution;
import com.microsoft.tfs.core.clients.versioncontrol.conflicts.resolutions.ConflictResolutionOptions;
import com.microsoft.tfs.util.Check;

/**
 * @since TEE-SDK-10.1
 */
public class CompositeConflictResolutionContributor implements ConflictResolutionContributor {
    private final List<ConflictResolutionContributor> contributors = new ArrayList<ConflictResolutionContributor>();

    public CompositeConflictResolutionContributor() {
    }

    public CompositeConflictResolutionContributor(final ConflictResolutionContributor[] subContributors) {
        Check.notNull(subContributors, "subContributors"); //$NON-NLS-1$

        addContributors(subContributors);
    }

    public void addContributor(final ConflictResolutionContributor subContributor) {
        Check.notNull(subContributor, "subContributor"); //$NON-NLS-1$
        contributors.add(subContributor);
    }

    public void addContributors(final ConflictResolutionContributor[] subContributors) {
        Check.notNull(subContributors, "subContributor"); //$NON-NLS-1$

        for (int i = 0; i < subContributors.length; i++) {
            addContributor(subContributors[i]);
        }
    }

    @Override
    public Collection<ConflictResolution> getConflictResolutions(
        final ConflictDescription conflictDescription,
        final ConflictResolutionOptions resolutionOptions) {
        final Collection<ConflictResolution> resolutions = new ArrayList<ConflictResolution>();

        for (final ConflictResolutionContributor contributor : contributors) {
            final Collection<ConflictResolution> contributorResolutions =
                contributor.getConflictResolutions(conflictDescription, resolutionOptions);

            if (contributorResolutions != null) {
                resolutions.addAll(contributorResolutions);
            }
        }

        return resolutions;
    }
}
