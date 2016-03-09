// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.artifact;

import com.microsoft.tfs.core.clients.registration.ToolNames;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlConstants;
import com.microsoft.tfs.core.clients.workitem.internal.InternalWorkItemConstants;

/**
 * Static methods to create {@link ArtifactID}s from changeset, item IDs, and
 * storyboard paths.
 *
 * @since TEE-SDK-10.1
 * @threadsafety immutable
 */
public class ArtifactIDFactory {
    public static ArtifactID newChangesetArtifactID(final int changesetID) {
        return new ArtifactID(
            ToolNames.VERSION_CONTROL,
            VersionControlConstants.CHANGESET_ARTIFACT_TYPE,
            String.valueOf(changesetID));
    }

    public static ArtifactID newLatestItemVersionArtifactID(final int itemID) {
        return new ArtifactID(
            ToolNames.VERSION_CONTROL,
            VersionControlConstants.LATEST_ITEM_ARTIFACT_TYPE,
            String.valueOf(itemID));
    }

    public static ArtifactID newStoryboardArtifactID(final String filePathOrUri) {
        return new ArtifactID(ToolNames.REQUIREMENTS, VersionControlConstants.STORYBOARD_ARTIFACT_TYPE, filePathOrUri);
    }

    public static ArtifactID newWorkItemArtifactID(final int id) {
        return new ArtifactID(
            ToolNames.WORK_ITEM_TRACKING,
            InternalWorkItemConstants.WORK_ITEM_ARTIFACT_TYPE,
            String.valueOf(id));
    }
}
