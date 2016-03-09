// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.commands.eclipse;

import java.text.MessageFormat;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;

import com.microsoft.tfs.client.common.framework.command.Command;
import com.microsoft.tfs.client.eclipse.Messages;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.LocaleUtil;

/**
 *
 *
 * @threadsafety unknown
 */
public class CloseProjectsCommand extends Command {
    private final IProject[] projects;

    public CloseProjectsCommand(final IProject[] projects) {
        Check.notNull(projects, "projects"); //$NON-NLS-1$

        this.projects = projects;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        if (projects.length == 1) {
            return MessageFormat.format(
                Messages.getString("CloseProjectsCommand.ClosingProjectFormat"), //$NON-NLS-1$
                projects[0].getName());
        } else {
            return MessageFormat.format(
                Messages.getString("CloseProjectsCommand.ClosingMultipleProjectsFormat"), //$NON-NLS-1$
                projects.length);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getErrorDescription() {
        return Messages.getString("CloseProjectsCommand.ErrorDescription"); //$NON-NLS-1$
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getLoggingDescription() {
        if (projects.length == 1) {
            return MessageFormat.format(
                Messages.getString("CloseProjectsCommand.ClosingProjectFormat", LocaleUtil.ROOT), //$NON-NLS-1$
                projects[0].getName());
        } else {
            return MessageFormat.format(
                Messages.getString("CloseProjectsCommand.ClosingMultipleProjectsFormat", LocaleUtil.ROOT), //$NON-NLS-1$
                projects.length);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected IStatus doRun(final IProgressMonitor progressMonitor) throws Exception {
        progressMonitor.beginTask(getName(), projects.length);

        for (int i = 0; i < projects.length; i++) {
            final IProgressMonitor projectMonitor = new SubProgressMonitor(progressMonitor, 1);

            projectMonitor.setTaskName(
                MessageFormat.format(
                    Messages.getString("CloseProjectsCommand.ClosingProjectFormat"), //$NON-NLS-1$
                    projects[i].getName()));

            try {
                projects[i].close(projectMonitor);
            } catch (final OperationCanceledException e) {
                return Status.CANCEL_STATUS;
            }

            if (progressMonitor.isCanceled()) {
                return Status.CANCEL_STATUS;
            }
        }

        return Status.OK_STATUS;
    }
}
