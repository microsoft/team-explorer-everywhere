// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.exceptions;

import java.text.MessageFormat;

import com.microsoft.tfs.core.Messages;

/**
 * Exception that is thrown when a build definition was not found.
 *
 *
 * @since TEE-SDK-10.1
 */
public class BuildDefinitionNotFoundException extends BuildException {

    public BuildDefinitionNotFoundException(final String teamProject, final String name) {
        super(MessageFormat.format(
            Messages.getString("BuildDefinitionNotFoundException.MessageFormat"), //$NON-NLS-1$
            teamProject,
            name));
    }

}
