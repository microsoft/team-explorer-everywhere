// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.ui;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.framework.background.IBackgroundTask;
import com.microsoft.tfs.client.common.framework.background.JobBackgroundTask;
import com.microsoft.tfs.client.common.framework.command.CommandFactory;
import com.microsoft.tfs.client.common.ui.autoconnect.UIAutoConnector;
import com.microsoft.tfs.client.common.ui.commands.ConnectToDefaultRepositoryCommand;
import com.microsoft.tfs.client.common.ui.framework.command.UIAsyncObjectWaiter;
import com.microsoft.tfs.client.common.ui.framework.command.UIJobCommandAdapter;
import com.microsoft.tfs.client.common.ui.protocolhandler.ProtocolHandler;
import com.microsoft.tfs.client.eclipse.TFSEclipseClientPlugin;

/**
 *
 *
 * @threadsafety unknown
 */
public class EclipseAutoConnector extends UIAutoConnector {
    private static final Log log = LogFactory.getLog(EclipseAutoConnector.class);

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean shouldAutoConnect() {
        /* Don't try to auto-connect if we have ever connected an IProject. */
        int nProjects = TFSEclipseClientPlugin.getDefault().getProjectManager().getProjects().length;
        int nClosedProjects = TFSEclipseClientPlugin.getDefault().getProjectManager().getClosedProjects().length;
        boolean hasProtocolHandlerRequest = ProtocolHandler.getInstance().hasProtocolHandlerRequest();
        return (nProjects == 0 && nClosedProjects == 0) || hasProtocolHandlerRequest;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected IStatus executeConnectCommand(final Shell shell, final ConnectToDefaultRepositoryCommand command) {
        final UIJobCommandAdapter connectJob =
            new UIJobCommandAdapter(CommandFactory.newCancelableCommand(command), null, null);

        /* Register with the server manager */
        final IBackgroundTask backgroundTask = new JobBackgroundTask(connectJob);

        TFSEclipseClientUIPlugin.getDefault().getServerManager().backgroundConnectionTaskStarted(backgroundTask);

        try {
            connectJob.schedule();

            try {
                new UIAsyncObjectWaiter().joinJob(connectJob);
            } catch (final InterruptedException e) {
                return new Status(IStatus.ERROR, TFSEclipseClientUIPlugin.PLUGIN_ID, 0, null, e);
            }

            /*
             * NOTE: using a UIJobCommandAdapter means that we cannot use the
             * job's result, we must look at the command's status.
             */
            return connectJob.getCommandStatus();
        } finally {
            TFSEclipseClientUIPlugin.getDefault().getServerManager().backgroundConnectionTaskFinished(backgroundTask);
        }
    }
}
