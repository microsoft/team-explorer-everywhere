// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.framework.command;

import org.eclipse.core.runtime.IStatus;

import com.microsoft.tfs.client.common.TFSCommonClientPlugin;
import com.microsoft.tfs.client.common.framework.status.TeamExplorerStatus;
import com.microsoft.tfs.client.common.framework.status.UncaughtCommandExceptionStatus;

public class PlatformLogCommandFinishedCallback implements ICommandFinishedCallback {
    /**
     * Any statuses with severities greater than this will be sent to the
     * Eclipse (platform) log. Note that
     * {@link LoggingCommandFinishedCallback#platformLogExceptionsOnly} is
     * checked before this value.
     */
    private final int minimumSeverity = -1;

    /**
     * Requires that any statuses require exceptions to go to the platform log.
     */
    private final boolean uncaughtExceptionsOnly = true;

    @Override
    public void onCommandFinished(final ICommand command, IStatus status) {
        /*
         * Log to the platform log if there was an uncaught exception, or if the
         * severity is greater than the minimum.
         */
        if ((uncaughtExceptionsOnly && status instanceof UncaughtCommandExceptionStatus)
            || (!uncaughtExceptionsOnly && status.getSeverity() >= minimumSeverity)) {
            /*
             * UncaughtCommandExceptionStatus is a TeamExplorerStatus, which
             * does a silly trick: it makes the exception it was given
             * accessible only through a non-standard method. This helps when
             * displaying the status to the user in a dialog, but prevents the
             * platform logger from logging stack traces, so fix it up here.
             *
             * The right long term fix is to ditch TeamExplorerStatus entirely.
             */
            if (status instanceof TeamExplorerStatus) {
                status = ((TeamExplorerStatus) status).toNormalStatus();
            }

            TFSCommonClientPlugin.getDefault().getLog().log(status);
        }
    }
}
