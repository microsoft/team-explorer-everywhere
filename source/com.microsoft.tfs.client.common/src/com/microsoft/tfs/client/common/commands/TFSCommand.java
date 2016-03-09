// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.commands;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;

import com.microsoft.tfs.client.common.TFSCommonClientPlugin;
import com.microsoft.tfs.client.common.framework.command.Command;
import com.microsoft.tfs.client.common.framework.command.CommandInitializationRunnable;
import com.microsoft.tfs.client.common.framework.command.exception.ICommandExceptionHandler;
import com.microsoft.tfs.client.common.framework.status.TeamExplorerStatus;
import com.microsoft.tfs.client.common.util.ProgressMonitorTaskMonitorAdapter;
import com.microsoft.tfs.core.exceptions.TECoreException;
import com.microsoft.tfs.core.httpclient.ActiveHttpMethods;
import com.microsoft.tfs.util.tasks.TaskMonitor;
import com.microsoft.tfs.util.tasks.TaskMonitorService;

/**
 * An extension of the basic command but with handling built in for common
 * exception cases in our client products.
 */
public abstract class TFSCommand extends Command {
    public TFSCommand() {
        addExceptionHandler(new TFSCommandExceptionHandler());

        addCommandInitializationRunnable(new TFSCommandInitializationRunnable());
    }

    protected static class TFSCommandExceptionHandler implements ICommandExceptionHandler {
        @Override
        public IStatus onException(final Throwable t) {
            if (t instanceof TECoreException) {
                final String exceptionMessage = getErrorMessage(t.getLocalizedMessage());
                return new TeamExplorerStatus(IStatus.ERROR, TFSCommonClientPlugin.PLUGIN_ID, 0, exceptionMessage, t);
            }

            return null;
        }

        public static String getErrorMessage(final String message) {
            return message.replaceFirst("^TF[\\d]+: ", ""); //$NON-NLS-1$ //$NON-NLS-2$
        }
    }

    private static class TFSCommandInitializationRunnable implements CommandInitializationRunnable {
        private volatile boolean addedTaskMonitorAdapter = false;

        @Override
        public void initialize(final IProgressMonitor progressMonitor) throws Exception {
            final TaskMonitor tm = new ProgressMonitorTaskMonitorAdapter(progressMonitor);
            TaskMonitorService.pushTaskMonitor(tm);
            ActiveHttpMethods.setMonitor(tm);
            addedTaskMonitorAdapter = true;
        }

        @Override
        public void complete(final IProgressMonitor progressMonitor) {
            if (addedTaskMonitorAdapter) {
                TaskMonitorService.popTaskMonitor(true);
                ActiveHttpMethods.clearMonitor();
            }
        }
    }
}
