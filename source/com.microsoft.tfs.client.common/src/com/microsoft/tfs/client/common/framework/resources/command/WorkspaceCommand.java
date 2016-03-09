// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.framework.resources.command;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.internal.resources.Workspace;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;

import com.microsoft.tfs.client.common.framework.command.CancellableCommand;
import com.microsoft.tfs.client.common.framework.command.CommandCancellableListener;
import com.microsoft.tfs.client.common.framework.command.CommandWrapper;
import com.microsoft.tfs.client.common.framework.command.ICancellableCommand;
import com.microsoft.tfs.client.common.framework.command.ICommand;
import com.microsoft.tfs.client.common.framework.command.exception.ICommandExceptionHandler;
import com.microsoft.tfs.util.Check;

/**
 * <p>
 * {@link WorkspaceCommand} wraps any {@link ICommand}, giving the wrapped
 * {@link ICommand} workspace batched semantics. Using {@link WorkspaceCommand}
 * is the equivalent, for example, of calling the
 * {@link IWorkspace#run(org.eclipse.core.resources.IWorkspaceRunnable, ISchedulingRule, int, IProgressMonitor)}
 * method inside a command.
 * </p>
 *
 * <p>
 * A {@link WorkspaceCommand} is associated with an {@link ISchedulingRule},
 * which will be passed to the
 * {@link IWorkspace#run(org.eclipse.core.resources.IWorkspaceRunnable, ISchedulingRule, int, IProgressMonitor)}
 * method. The default scheduling rule is the workspace root - this should be
 * overridden when appropriate.
 * </p>
 *
 * <p>
 * See the {@link #run(IProgressMonitor)} command's Javadoc for an important
 * warning.
 * </p>
 */
public class WorkspaceCommand extends CancellableCommand implements CommandWrapper {
    private final static Log log = LogFactory.getLog(WorkspaceCommand.class);

    /**
     * The wrapped {@link ICommand} (never <code>null</code>).
     */
    private final ICommand wrappedCommand;

    /**
     * The {@link ISchedulingRule} rule to use when running the command (never
     * <code>null</code>).
     */
    private final ISchedulingRule schedulingRule;

    /**
     * Creates a new {@link WorkspaceCommand} that wraps the specified
     * {@link ICommand} and uses the default scheduling rule.
     *
     * @param wrappedCommand
     *        an {@link ICommand} to wrap (must not be <code>null</code>)
     */
    public WorkspaceCommand(final ICommand wrappedCommand) {
        this(wrappedCommand, null);
    }

    /**
     * Creates a new {@link WorkspaceCommand} that wraps the specified
     * {@link ICommand} and uses the specified {@link ISchedulingRule}.
     *
     * @param wrappedCommand
     *        an {@link ICommand} to wrap (must not be <code>null</code>)
     * @param schedulingRule
     *        an {@link ISchedulingRule} rule to use, or <code>null</code> to
     *        use the default scheduling rule (the workspace root)
     */
    public WorkspaceCommand(final ICommand wrappedCommand, ISchedulingRule schedulingRule) {
        Check.notNull(wrappedCommand, "wrappedCommand"); //$NON-NLS-1$

        this.wrappedCommand = wrappedCommand;
        if (schedulingRule == null) {
            schedulingRule = ResourcesPlugin.getWorkspace().getRoot();
        }
        this.schedulingRule = schedulingRule;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.microsoft.tfs.client.common.shared.command.ICommand#
     * getExceptionHandler ()
     */
    @Override
    public ICommandExceptionHandler getExceptionHandler() {
        return wrappedCommand.getExceptionHandler();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.microsoft.tfs.client.common.shared.command.ICommand#getName()
     */
    @Override
    public String getName() {
        return wrappedCommand.getName();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.microsoft.tfs.client.common.shared.command.ICommand#
     * getErrorDescription ()
     */
    @Override
    public String getErrorDescription() {
        return wrappedCommand.getErrorDescription();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.microsoft.tfs.client.common.framework.command.ICommand#
     * getLoggingDescription()
     */
    @Override
    public String getLoggingDescription() {
        return wrappedCommand.getLoggingDescription();
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.microsoft.tfs.client.common.shared.command.ICommand#isCancelable()
     */
    @Override
    public boolean isCancellable() {
        return wrappedCommand.isCancellable();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addCancellableChangedListener(final CommandCancellableListener listener) {
        if (wrappedCommand instanceof ICancellableCommand) {
            ((ICancellableCommand) wrappedCommand).addCancellableChangedListener(listener);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeCancellableChangedListener(final CommandCancellableListener listener) {
        if (wrappedCommand instanceof ICancellableCommand) {
            ((ICancellableCommand) wrappedCommand).removeCancellableChangedListener(listener);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see com.microsoft.tfs.client.common.framework.command.CommandWrapper#
     * getWrappedCommand()
     */
    @Override
    public ICommand getWrappedCommand() {
        return wrappedCommand;
    }

    /**
     * {@inheritDoc}
     *
     * <b>Aesthetic Warning</b>
     * <p>
     * This method calls
     * {@link IWorkspace#run(org.eclipse.core.resources.IWorkspaceRunnable, ISchedulingRule, int, IProgressMonitor)}
     * , which opens a new sub-monitor on the given {@link IProgressMonitor}, in
     * order to display its "Refreshing /some/path" messages. Howerver, the
     * {@link Workspace} implementation always forces the
     * {@link SubProgressMonitor#PREPEND_MAIN_LABEL_TO_SUBTASK} style bit, which
     * prepends any existing label the task monitor had (like "Downloading") to
     * the refresh sublabel, resulting in something ugly like
     * "Downloading Refreshing /some/path" appearing in the dialog. You might
     * want to make sure the existing label ends in a colon or some other
     * separator so it looks good in the dialog, or pass in a
     * {@link NullProgressMonitor} if you just don't care to see refreshing
     * status.
     * <p>
     */
    @Override
    public IStatus run(final IProgressMonitor progressMonitor) throws Exception {
        final WorkspaceRunnableCommandAdapter adapter = new WorkspaceRunnableCommandAdapter(wrappedCommand);

        try {
            ResourcesPlugin.getWorkspace().run(adapter, schedulingRule, IResource.NONE, progressMonitor);
        } catch (final CoreException e) {
            log.error("Workspace command error", e.getStatus().getException()); //$NON-NLS-1$

            throw (Exception) e.getStatus().getException();
        }

        return adapter.getStatus();
    }
}
