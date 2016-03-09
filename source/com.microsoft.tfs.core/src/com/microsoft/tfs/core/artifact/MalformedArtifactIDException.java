// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.artifact;

import java.text.MessageFormat;

import com.microsoft.tfs.core.exceptions.TECoreException;

/**
 * Thrown when an artifact ID is not in the proper format.
 *
 * @since TEE-SDK-10.1
 * @threadsafety thread-safe
 */
public class MalformedArtifactIDException extends TECoreException {
    public MalformedArtifactIDException(final ArtifactID id) {
        super(MessageFormat.format(
            "Malformed artifact id: tool=[{0}] artifactType=[{1}] toolSpecificId=[{2}]", //$NON-NLS-1$
            id.getTool(),
            id.getArtifactType(),
            id.getToolSpecificID()));
    }
}
