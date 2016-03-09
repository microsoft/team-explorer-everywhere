// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.exceptions;

import java.text.MessageFormat;

import com.microsoft.tfs.core.Messages;

/**
 * Exception that is thrown when a build agent was not found at the specified
 * URI.
 *
 * @since TEE-SDK-10.1
 */
public class BuildAgentNotFoundForURIException extends BuildException {
    public BuildAgentNotFoundForURIException(final String buildAgentUri, final String userName) {
        super(MessageFormat.format(
            Messages.getString("BuildAgentNotFoundForUriException.MessageFormat"), //$NON-NLS-1$
            buildAgentUri,
            userName));
    }

}
