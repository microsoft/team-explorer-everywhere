// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.exceptions;

import java.text.MessageFormat;

import com.microsoft.tfs.core.Messages;

/**
 * Exception that is thrown when a build stop request failed.
 *
 * @since TEE-SDK-10.1
 */
public class StopBuildFailedException extends BuildException {
    private final String uri;
    private final String failureMessage;

    public StopBuildFailedException(final String uri, final String failureMessage) {
        super(MessageFormat.format(Messages.getString("StopBuildFailedException.MessageFormat"), uri, failureMessage)); //$NON-NLS-1$
        this.uri = uri;
        this.failureMessage = failureMessage;
    }

    /**
     * @return the uri
     */
    public String getURI() {
        return uri;
    }

    /**
     * @return the failureMessage
     */
    public String getFailureMessage() {
        return failureMessage;
    }

}
