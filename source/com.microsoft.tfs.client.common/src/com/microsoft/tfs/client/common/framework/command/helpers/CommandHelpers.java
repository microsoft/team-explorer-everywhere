// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.framework.command.helpers;

import java.text.MessageFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.client.common.framework.command.CommandWrapper;
import com.microsoft.tfs.client.common.framework.command.ICommand;

public class CommandHelpers {
    private static final Log log = LogFactory.getLog(CommandHelpers.class);

    private final static int MAX_RECURSION_DEPTH = 32;

    /**
     * Gets the type of the underlying command class.
     *
     * Note that we occasionally wrap commands inside other commands in order to
     * provide additional functionality. See, for example,
     * ThreadedCancellableCommand. We want to report the ultimate cause of this
     * error, so we dig until we locate the source. Limit to a suitable
     * iteration depth to avoid infinite recursion.
     *
     * @param command
     *        The command being executed (not <code>null</code>)
     * @return the underlying (unwrapped) command
     */
    public static ICommand unwrapCommand(ICommand command) {
        for (int i = 0; i < MAX_RECURSION_DEPTH && command instanceof CommandWrapper; i++) {
            final ICommand wrappedCommand = ((CommandWrapper) command).getWrappedCommand();

            if (wrappedCommand == null) {
                log.error(
                    MessageFormat.format("Command {0} wraps null command", command.getClass().getCanonicalName())); //$NON-NLS-1$
                break;
            }

            command = wrappedCommand;
        }

        return command;
    }
}
