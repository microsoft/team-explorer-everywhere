// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.commands;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.microsoft.tfs.client.common.framework.command.exception.ICommandExceptionHandler;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.jni.internal.ntlm.NTLMVersionException;

/**
 * Exception handlers for the connect to server commands.
 *
 * @threadsafety unknown
 */
public final class ConnectCommandExceptionHandler implements ICommandExceptionHandler {
    @Override
    public IStatus onException(final Throwable t) {
        if (t instanceof NTLMVersionException) {
            return new Status(IStatus.ERROR, TFSCommonUIClientPlugin.PLUGIN_ID, 0, t.getLocalizedMessage(), null);
        }

        return null;
    }
}