// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.conflicts.resolutions.contributors;

import java.util.Collection;

import com.microsoft.tfs.core.clients.versioncontrol.conflicts.ConflictDescription;
import com.microsoft.tfs.core.clients.versioncontrol.conflicts.resolutions.ConflictResolution;
import com.microsoft.tfs.core.clients.versioncontrol.conflicts.resolutions.ConflictResolutionOptions;

/**
 * @since TEE-SDK-10.1
 */
public interface ConflictResolutionContributor {
    /**
     * Returns a list of contributed {@link ConflictResolution}s (eg, external
     * merge tools, internal merge tools, etc.) that are available to this
     * {@link ConflictDescription}.
     *
     * @param conflictDescription
     *        The {@link ConflictDescription} being resolved
     * @param resolutionOptions
     *        The {@link ConflictResolutionOptions}s for this resolution
     * @return A collection of conflict resolutions (never <code>null</code>)
     */
    public Collection<ConflictResolution> getConflictResolutions(
        ConflictDescription conflictDescription,
        ConflictResolutionOptions resolutionOptions);
}
