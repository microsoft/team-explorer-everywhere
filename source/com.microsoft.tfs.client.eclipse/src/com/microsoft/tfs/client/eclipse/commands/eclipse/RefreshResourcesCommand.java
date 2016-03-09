// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.commands.eclipse;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;

import com.microsoft.tfs.client.common.framework.command.Command;
import com.microsoft.tfs.client.eclipse.Messages;
import com.microsoft.tfs.client.eclipse.TFSEclipseClientPlugin;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.LocaleUtil;

public class RefreshResourcesCommand extends Command {
    private final IResource[] resources;

    public RefreshResourcesCommand(final IResource[] resources) {
        Check.notNull(resources, "resources"); //$NON-NLS-1$

        this.resources = resources;
    }

    @Override
    public String getName() {
        if (resources.length == 1) {
            return MessageFormat.format(
                Messages.getString("RefreshResourcesCommand.RefreshingSingleResourceFormat"), //$NON-NLS-1$
                resources[0].getName());
        } else {
            return Messages.getString("RefreshResourcesCommand.RefreshingMultipleResources"); //$NON-NLS-1$
        }
    }

    @Override
    public String getErrorDescription() {
        if (resources.length == 1) {
            return Messages.getString("RefreshResourcesCommand.ErrorRefreshingMultipleResources"); //$NON-NLS-1$
        } else {
            return Messages.getString("RefreshResourcesCommand.ErrorRefreshingSingleResource"); //$NON-NLS-1$
        }
    }

    @Override
    public String getLoggingDescription() {
        if (resources.length == 1) {
            return MessageFormat.format(
                Messages.getString("RefreshResourcesCommand.RefreshingSingleResourceFormat", LocaleUtil.ROOT), //$NON-NLS-1$
                resources[0].getName());
        } else {
            return Messages.getString("RefreshResourcesCommand.RefreshingMultipleResources", LocaleUtil.ROOT); //$NON-NLS-1$
        }
    }

    @Override
    protected IStatus doRun(final IProgressMonitor progressMonitor) throws Exception {
        progressMonitor.beginTask(getName(), resources.length);

        final List<IStatus> errors = new ArrayList<IStatus>();

        try {
            for (int i = 0; i < resources.length; i++) {
                final IResource resource = resources[i];
                final int depth =
                    (resource.getType() == IResource.FILE) ? IResource.DEPTH_ZERO : IResource.DEPTH_INFINITE;

                try {
                    resource.refreshLocal(depth, new SubProgressMonitor(progressMonitor, 1));
                } catch (final Exception e) {
                    errors.add(
                        new Status(IStatus.ERROR, TFSEclipseClientPlugin.PLUGIN_ID, 0, e.getLocalizedMessage(), e));
                }
            }
        } finally {
            progressMonitor.done();
        }

        if (errors.size() == 0) {
            return Status.OK_STATUS;
        }

        return new MultiStatus(
            TFSEclipseClientPlugin.PLUGIN_ID,
            0,
            errors.toArray(new IStatus[errors.size()]),
            getErrorDescription(),
            null);
    }
}
