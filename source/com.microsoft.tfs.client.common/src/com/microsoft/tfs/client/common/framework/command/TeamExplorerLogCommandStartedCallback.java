// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.framework.command;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.client.common.framework.command.helpers.CommandHelpers;
import com.microsoft.tfs.util.Check;

public class TeamExplorerLogCommandStartedCallback implements ICommandStartedCallback {
    @Override
    public void onCommandStarted(final ICommand command) {
        Check.notNull(command, "command"); //$NON-NLS-1$

        /*
         * Don't have a static Log for this class, because then the errors show
         * up as coming from this class, which is not correct. Instead, get the
         * logger for the class the errors originated from. Note that this is
         * not a particularly expensive operation - it looks up the classname in
         * a hashmap. This should be more than suitable for a command finished
         * error handler.
         */
        final Log log = LogFactory.getLog(CommandHelpers.unwrapCommand(command).getClass());

        final String logMessage = command.getLoggingDescription();

        if (logMessage != null) {
            log.info(logMessage);
        }
    }
}
