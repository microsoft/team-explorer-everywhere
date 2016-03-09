// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.ui.commands.sync;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.subscribers.Subscriber;

import com.microsoft.tfs.client.common.commands.TFSCommand;
import com.microsoft.tfs.client.eclipse.ui.Messages;
import com.microsoft.tfs.client.eclipse.ui.TFSEclipseClientUIPlugin;
import com.microsoft.tfs.util.LocaleUtil;

public class RefreshSubscriberCommand extends TFSCommand {
    private final Subscriber subscriber;
    private final IResource[] roots;
    private final int depth;

    public RefreshSubscriberCommand(final Subscriber subscriber) {
        this(subscriber, subscriber.roots(), IResource.DEPTH_INFINITE);
    }

    public RefreshSubscriberCommand(final Subscriber subscriber, final IResource[] roots, final int depth) {
        this.subscriber = subscriber;
        this.roots = roots;
        this.depth = depth;
    }

    @Override
    public String getName() {
        return (Messages.getString("RefreshSubscriberCommand.CommandText")); //$NON-NLS-1$
    }

    @Override
    public String getErrorDescription() {
        return (Messages.getString("RefreshSubscriberCommand.ErrorText")); //$NON-NLS-1$
    }

    @Override
    public String getLoggingDescription() {
        return (Messages.getString("RefreshSubscriberCommand.CommandText", LocaleUtil.ROOT)); //$NON-NLS-1$
    }

    @Override
    protected IStatus doRun(final IProgressMonitor progressMonitor) throws Exception {
        try {
            subscriber.refresh(roots, depth, progressMonitor);
        } catch (final TeamException e) {
            return new Status(
                Status.ERROR,
                TFSEclipseClientUIPlugin.PLUGIN_ID,
                TeamException.UNABLE,
                Messages.getString("RefreshSubscriberCommand.CouldNotRefreshSubscribers"), //$NON-NLS-1$
                e);
        }

        return Status.OK_STATUS;
    }
}
