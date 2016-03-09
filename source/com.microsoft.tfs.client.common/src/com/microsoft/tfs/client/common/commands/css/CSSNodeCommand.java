// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.commands.css;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.microsoft.tfs.client.common.TFSCommonClientPlugin;
import com.microsoft.tfs.client.common.commands.TFSConnectedCommand;
import com.microsoft.tfs.core.exceptions.TECoreException;

public abstract class CSSNodeCommand extends TFSConnectedCommand {
    protected CSSNodeCommand() {
        addExceptionHandler(new CSSNodeCommandExceptionHandler());
    }

    /**
     * Turns CSS node errors (with the silly ---> messages) into standard
     * message error statuses. Ignores others, delegates them upward.
     */
    private final class CSSNodeCommandExceptionHandler extends TFSCommandExceptionHandler {
        @Override
        public IStatus onException(final Throwable t) {
            if (t instanceof TECoreException) {
                final String exceptionMessage = getErrorMessage(t.getLocalizedMessage());

                if (exceptionMessage.indexOf("--->") > 0) //$NON-NLS-1$
                {
                    final String userFriendlyMessage = exceptionMessage.substring(0, exceptionMessage.indexOf("--->")); //$NON-NLS-1$
                    return new Status(IStatus.ERROR, TFSCommonClientPlugin.PLUGIN_ID, 0, userFriendlyMessage, t);
                }
            }

            return null;
        }
    }
}
