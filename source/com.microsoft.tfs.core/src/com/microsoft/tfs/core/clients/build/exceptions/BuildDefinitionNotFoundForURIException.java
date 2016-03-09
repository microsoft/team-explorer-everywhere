// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.exceptions;

import java.text.MessageFormat;

import com.microsoft.tfs.core.Messages;

/**
 * Exception that is thrown when a build definition was not found at the
 * specified URI.
 *
 * @since TEE-SDK-10.1
 */
public class BuildDefinitionNotFoundForURIException extends BuildException {
    public BuildDefinitionNotFoundForURIException(final String buildAgentUri, final String userName) {
        super(MessageFormat.format(
            Messages.getString("BuildDefinitionNotFoundForUriException.MessageFormat"), //$NON-NLS-1$
            buildAgentUri,
            userName));
    }
}
