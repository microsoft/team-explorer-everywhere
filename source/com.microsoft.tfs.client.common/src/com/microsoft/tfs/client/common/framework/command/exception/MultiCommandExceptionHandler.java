// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.framework.command.exception;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IStatus;

import com.microsoft.tfs.util.Check;

public final class MultiCommandExceptionHandler implements ICommandExceptionHandler {
    private final ICommandExceptionHandler[] exceptionHandlers;

    private final static Log log = LogFactory.getLog(MultiCommandExceptionHandler.class);

    public MultiCommandExceptionHandler(final ICommandExceptionHandler[] exceptionHandlers) {
        Check.notNullOrEmpty(exceptionHandlers, "exceptionHandlers"); //$NON-NLS-1$

        this.exceptionHandlers = exceptionHandlers;
    }

    @Override
    public IStatus onException(final Throwable t) {
        for (int i = 0; i < exceptionHandlers.length; i++) {
            IStatus exceptionStatus;

            try {
                exceptionStatus = exceptionHandlers[i].onException(t);
            } catch (final Throwable handlerException) {
                log.error("Caught exception while trying to handle command exception", handlerException); //$NON-NLS-1$

                exceptionStatus = null;
            }

            if (exceptionStatus != null) {
                return exceptionStatus;
            }
        }

        return null;
    }
}
