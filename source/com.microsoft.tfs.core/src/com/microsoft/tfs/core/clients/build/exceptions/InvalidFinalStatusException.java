// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.exceptions;

import java.text.MessageFormat;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.core.clients.build.IBuildServer;
import com.microsoft.tfs.core.clients.build.flags.BuildStatus;

@SuppressWarnings("serial")
public class InvalidFinalStatusException extends BuildException {

    public InvalidFinalStatusException(
        final String buildNumber,
        final BuildStatus invalidStatus,
        final IBuildServer buildServer) {
        super(MessageFormat.format(
            Messages.getString("InvalidFinalStatusException.Format"), //$NON-NLS-1$
            buildNumber,
            buildServer.getDisplayText(invalidStatus),
            buildServer.getDisplayText(BuildStatus.FAILED),
            buildServer.getDisplayText(BuildStatus.PARTIALLY_SUCCEEDED),
            buildServer.getDisplayText(BuildStatus.STOPPED),
            buildServer.getDisplayText(BuildStatus.SUCCEEDED)));
    }

}
