// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.exceptions;

@SuppressWarnings("serial")
public class QueuedBuildDoesNotExistException extends BuildException {

    public QueuedBuildDoesNotExistException(final String message) {
        super(message);
    }
}
