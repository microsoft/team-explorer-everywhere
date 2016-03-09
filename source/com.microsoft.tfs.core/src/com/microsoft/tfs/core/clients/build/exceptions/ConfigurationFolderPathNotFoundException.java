// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.exceptions;

import java.text.MessageFormat;

import com.microsoft.tfs.core.Messages;

/**
 * Exception that is thrown when a configuration folder path was not found.
 *
 * @since TEE-SDK-10.1
 */
public class ConfigurationFolderPathNotFoundException extends BuildException {
    public ConfigurationFolderPathNotFoundException(final String configurationFolderPath) {
        super(MessageFormat.format(
            Messages.getString("ConfigurationFolderPathNotFoundException.MessageFormat"), //$NON-NLS-1$
            configurationFolderPath));
    }
}
