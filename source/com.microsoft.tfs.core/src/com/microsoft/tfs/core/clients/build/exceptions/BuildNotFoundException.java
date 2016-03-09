// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.exceptions;

import java.text.MessageFormat;

import com.microsoft.tfs.core.Messages;

/**
 * Exception that is thrown when a build was not found.
 *
 * @since TEE-SDK-10.1
 */
public class BuildNotFoundException extends BuildException {

    public BuildNotFoundException(final String buildNumber, final String buildDefinitionPath) {
        super(MessageFormat.format(
            Messages.getString("BuildNotFoundException.MessageFormat"), //$NON-NLS-1$
            buildNumber,
            buildDefinitionPath));
    }

}
