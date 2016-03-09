// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.framework.command.exception;

import org.eclipse.core.runtime.IStatus;

/**
 * A command exception handler that can be used when no other command exception
 * handlers are available for an ICommand.
 *
 * @threadsafety unknown
 */
public class NullCommandExceptionHandler implements ICommandExceptionHandler {
    @Override
    public IStatus onException(final Throwable t) {
        return null;
    }
}
