// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.exceptions;

/**
 * Exception that is thrown when a configuration folder was not found.
 *
 * @since TEE-SDK-10.1
 */
public class ConfigurationFolderNotFoundException extends BuildException {
    public ConfigurationFolderNotFoundException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public ConfigurationFolderNotFoundException(final String message) {
        super(message);
    }

    public ConfigurationFolderNotFoundException(final Throwable cause) {
        super(cause);
    }

}
