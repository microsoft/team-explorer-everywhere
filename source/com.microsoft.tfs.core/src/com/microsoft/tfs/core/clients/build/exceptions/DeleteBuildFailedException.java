// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.exceptions;

import java.text.MessageFormat;

import com.microsoft.tfs.core.Messages;

/**
 * Exception that is thrown when a build delete request failed.
 *
 * @since TEE-SDK-10.1
 */
public class DeleteBuildFailedException extends BuildException {

    private final String build;
    private final String failureMessage;

    public DeleteBuildFailedException(final String build, final String failureMessage) {
        super(MessageFormat.format(
            Messages.getString("DeleteBuildFailedException.MessageFormat"), //$NON-NLS-1$
            build,
            failureMessage));
        this.build = build;
        this.failureMessage = failureMessage;
    }

    /**
     * @return the build
     */
    public String getBuild() {
        return build;
    }

    /**
     * @return the failureMessage
     */
    public String getFailureMessage() {
        return failureMessage;
    }

}
