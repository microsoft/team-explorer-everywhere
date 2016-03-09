// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.exceptions;

/**
 * Exception thrown when parsing the BuildType file. The TFSBuild.proj file is
 * parse when querying build definitions belonging to a TSF2005 server.
 *
 * @since TEE-SDK-10.1
 */
public class BuildTypeFileParseException extends BuildException {
    public BuildTypeFileParseException() {
        super();
    }

    public BuildTypeFileParseException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public BuildTypeFileParseException(final String message) {
        super(message);
    }

    public BuildTypeFileParseException(final Throwable cause) {
        super(cause);
    }

}
