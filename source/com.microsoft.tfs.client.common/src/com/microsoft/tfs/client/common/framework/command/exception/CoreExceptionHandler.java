// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.framework.command.exception;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;

import com.microsoft.tfs.core.telemetry.TfsTelemetryHelper;

/**
 * An {@link ICommandExceptionHandler} that handles {@link CoreException}s. They
 * are handled by returning their {@link IStatus} available from
 * {@link CoreException#getStatus()}.
 *
 * @see ICommandExceptionHandler
 * @see CoreException
 */
public class CoreExceptionHandler implements ICommandExceptionHandler {
    private static final Log log = LogFactory.getLog(CoreExceptionHandler.class);

    /*
     * (non-Javadoc)
     *
     * @seecom.microsoft.tfs.client.common.ui.shared.command.exception.
     * CommandExceptionHandler#doHandleException(java.lang.Throwable,
     * com.microsoft.tfs.client.common.ui.shared.command.ICommand)
     */
    @Override
    public IStatus onException(final Throwable t) {
        log.error(t.getMessage(), t);

        if (t instanceof CoreException) {
            TfsTelemetryHelper.sendException((CoreException) t);

            return ((CoreException) t).getStatus();
        }

        return null;
    }
}
