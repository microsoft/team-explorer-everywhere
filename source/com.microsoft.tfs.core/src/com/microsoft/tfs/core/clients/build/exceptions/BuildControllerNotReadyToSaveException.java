// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.exceptions;

import java.text.MessageFormat;

import com.microsoft.tfs.core.Messages;

@SuppressWarnings("serial")
public class BuildControllerNotReadyToSaveException extends BuildException {
    public BuildControllerNotReadyToSaveException(final String name) {
        super(MessageFormat.format(Messages.getString("BuildControllerNotReadyToSaveException.Format"), name)); //$NON-NLS-1$
    }
}
