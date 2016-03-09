// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.action;

import java.text.MessageFormat;

import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.ErrorDialog;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.framework.telemetry.ClientTelemetryHelper;

public abstract class ExtendedAction extends ObjectActionDelegate {
    private String name;
    private String errorMessage;

    public String getName() {
        if (name != null) {
            return name;
        }

        String className = getClass().getName();

        if (className.indexOf(".") != -1) //$NON-NLS-1$
        {
            className = className.substring(className.lastIndexOf(".") + 1); //$NON-NLS-1$
        }

        return className;
    }

    protected final void setName(final String name) {
        this.name = name;
    }

    public String getErrorMessage() {
        if (errorMessage != null) {
            return errorMessage;
        }

        final String messageFormat = Messages.getString("ExtendedAction.CouldNotExecuteFormat"); //$NON-NLS-1$
        return MessageFormat.format(messageFormat, getName());
    }

    protected final void setErrorMessage(final String errorMessage) {
        this.errorMessage = errorMessage;
    }

    @Override
    public final void run(final IAction action) {
        try {
            ClientTelemetryHelper.sendRunActionEvent(this);
            doRun(action);
        } catch (final Throwable t) {
            final String messageFormat = Messages.getString("ExtendedAction.ErrorDialogTitleFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, getName());
            LogFactory.getLog(this.getClass()).error(message, t);

            /* TODO: use StatusHelper here */
            ErrorDialog.openError(
                getShell(),
                message,
                null,
                new Status(IStatus.ERROR, TFSCommonUIClientPlugin.PLUGIN_ID, 0, getErrorMessage(), t));
        }
    }

    public abstract void doRun(IAction action);
}
