// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.framework.command.exception;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;

import com.microsoft.tfs.client.common.TFSCommonClientPlugin;
import com.microsoft.tfs.core.ws.runtime.exceptions.TransportRequestHandlerCanceledException;

/**
 * An {@link ICommandExceptionHandler} that handles {@link InterruptedException}
 * s and {@link OperationCanceledException}s. They are handled by returning an
 * {@link IStatus} with severity {@link IStatus#CANCEL}.
 *
 * @see ICommandExceptionHandler
 * @see InterruptedException
 * @see OperationCanceledException
 */
public class StandardCancellationExceptionHandler implements ICommandExceptionHandler {
    @Override
    public IStatus onException(final Throwable t) {
        if (t instanceof InterruptedException
            || t instanceof OperationCanceledException
            || t instanceof TransportRequestHandlerCanceledException) {
            return new Status(IStatus.CANCEL, TFSCommonClientPlugin.PLUGIN_ID, 0, null, t);
        }

        return null;
    }
}
