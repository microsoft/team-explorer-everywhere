// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.link;

import com.microsoft.tfs.core.artifact.ArtifactID;

/**
 * @since TEE-SDK-10.1
 */
public interface ExternalLink extends Link {
    public String getURI();

    public ArtifactID getArtifactID();
}
