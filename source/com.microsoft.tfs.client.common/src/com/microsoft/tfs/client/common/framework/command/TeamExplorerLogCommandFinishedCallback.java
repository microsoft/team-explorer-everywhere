// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.framework.command;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IStatus;

import com.microsoft.tfs.client.common.framework.command.helpers.CommandFinishedCallbackHelpers;
import com.microsoft.tfs.client.common.framework.command.helpers.CommandHelpers;
import com.microsoft.tfs.client.common.framework.status.TeamExplorerStatus;

/**
 * Handles logging to the Team Explorer log (previously called the "private log"
 * in code.) Maps IStatus severity to commons logging severity, thus the commons
 * logging configuration will decide what messages are logged.
 *
 * @threadsafety not thread safe
 */
public class TeamExplorerLogCommandFinishedCallback implements ICommandFinishedCallback {
    /**
     * Any statuses with severities greater than this will be sent to the Team
     * Explorer log. Note that these are mapped from IStatus severity -> commons
     * logging severity. Thus the default behavior is to log every status
     * *depending on the logging configuration*. It is recommended not to
     * reconfigure this.
     */
    private final int minimumSeverity = -1;

    @Override
    public void onCommandFinished(final ICommand command, IStatus status) {
        /*
         * Team Explorer ("private") logging. Map IStatus severity to commons
         * logging severity iff the severity is greater than the configured
         * tfsLogMinimumSeverity.
         */
        if (status.getSeverity() >= minimumSeverity) {
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

            /*
             * Don't have a static Log for this class, because then the errors
             * show up as coming from this class, which is not correct. Instead,
             * get the logger for the class the errors originated from. Note
             * that this is not a particularly expensive operation - it looks up
             * the classname in a hashmap. This should be more than suitable for
             * a command finished error handler.
             */
            final Log log = LogFactory.getLog(CommandHelpers.unwrapCommand(command).getClass());

            if (status.getSeverity() == IStatus.ERROR && log.isErrorEnabled()) {
                log.error(CommandFinishedCallbackHelpers.getMessageForStatus(status), status.getException());
            } else if (status.getSeverity() == IStatus.WARNING && log.isWarnEnabled()) {
                log.error(CommandFinishedCallbackHelpers.getMessageForStatus(status), status.getException());
            } else if (status.getSeverity() == IStatus.INFO && log.isInfoEnabled()) {
                log.info(CommandFinishedCallbackHelpers.getMessageForStatus(status), status.getException());
            } else if (status.getSeverity() == IStatus.CANCEL && log.isInfoEnabled()) {
                log.info(CommandFinishedCallbackHelpers.getMessageForStatus(status), status.getException());
            } else if (status.getSeverity() != IStatus.OK && log.isDebugEnabled()) {
                log.debug(CommandFinishedCallbackHelpers.getMessageForStatus(status), status.getException());
            } else if (log.isTraceEnabled()) {
                log.trace(CommandFinishedCallbackHelpers.getMessageForStatus(status), status.getException());
            }
        }
    }
}
