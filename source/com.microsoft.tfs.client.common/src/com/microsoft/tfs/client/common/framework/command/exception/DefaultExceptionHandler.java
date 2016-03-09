// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.framework.command.exception;

import java.text.MessageFormat;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.microsoft.tfs.client.common.Messages;
import com.microsoft.tfs.client.common.TFSCommonClientPlugin;
import com.microsoft.tfs.client.common.framework.command.ExtendedStatus;
import com.microsoft.tfs.client.common.framework.command.ICommand;
import com.microsoft.tfs.client.common.framework.status.UncaughtCommandExceptionStatus;
import com.microsoft.tfs.core.telemetry.TfsTelemetryHelper;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.tasks.CanceledException;

/**
 * An {@link ICommandExceptionHandler} that is used as a "last-resort" handler
 * to handle any exception not handled by any other handler. The exception is
 * handled by converting it into an {@link ExtendedStatus} with severity
 * {@link IStatus#ERROR} and the extended status flags
 * {@link ExtendedStatus#SHOW} and {@link ExtendedStatus#LOG}.
 *
 * @see ICommandExceptionHandler
 * @see ExtendedStatus
 */
public class DefaultExceptionHandler implements ICommandExceptionHandler {
    private final ICommand command;

    public DefaultExceptionHandler(final ICommand command) {
        Check.notNull(command, "command"); //$NON-NLS-1$

        this.command = command;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IStatus onException(final Throwable t) {
        if (t instanceof CanceledException || t instanceof InterruptedException) {
            return Status.CANCEL_STATUS;
        }

        if (t instanceof Exception) {
            TfsTelemetryHelper.sendException((Exception) t);
        } else {
            TfsTelemetryHelper.sendException(new Exception(t));
        }

        String details = t.getLocalizedMessage();
        if (details == null || details.length() == 0) {
            details = MessageFormat.format(
                Messages.getString("DefaultExceptionHandler.UnhandledExceptionPleaseCheckLogsMessageFormat"), //$NON-NLS-1$
                t.getClass().getSimpleName());
        }

        final String message =
            MessageFormat.format(
                Messages.getString("DefaultExceptionHandler.ErrorDescriptionColonDetailsFormat"), //$NON-NLS-1$
                command.getErrorDescription(),
                details);

        return new UncaughtCommandExceptionStatus(IStatus.ERROR, TFSCommonClientPlugin.PLUGIN_ID, 0, message, t);
    }
}
