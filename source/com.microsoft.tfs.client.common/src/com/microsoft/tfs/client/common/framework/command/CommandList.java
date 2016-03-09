// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.framework.command;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;

/**
 * <p>
 * {@link CommandList} is an {@link ICommand} implementation that holds a list
 * of sub-{@link ICommand}s. When the {@link CommandList} is run, it uses a
 * {@link MultiCommandHelper} to run all of the subcommands in the list.
 * </p>
 *
 * <p>
 * This class is only useful when the list of commands to run can be entirely
 * determined before running. This is often not the case. For example, the
 * output of a command may be the input to a subsequent command, or the list of
 * commands to run may not be able to be determined ahead of time. In these
 * cases, instead of using {@link CommandList}, write a custom {@link ICommand}
 * implementation that uses {@link MultiCommandHelper} internally.
 * </p>
 *
 * <p>
 * By default a {@link CommandList} instance is cancelable (
 * {@link ICommand#isCancellable()} returns <code>true</code>). You can call
 * {@link #setCancelable(boolean)} to modify this behavior.
 * </p>
 *
 * @see ICommand
 * @see MultiCommandHelper
 */
public final class CommandList extends Command {
    private final String name;
    private final String errorDescription;

    private final List commandList = new ArrayList();
    private final int[] continuableSeverities;
    private boolean rollback;

    /**
     * <p>
     * Creates a new {@link CommandList}, which is initially empty. Commands are
     * then added by calling {@link #addCommand(ICommand)}.
     * </p>
     *
     * <p>
     * This constructor produces a {@link CommandList} that only continues on
     * severity {@link IStatus#OK}. For more information on continuable
     * severities, see the documentation of the constructor that takes
     * continuable severities.
     * </p>
     *
     * @param name
     *        the name to return from {@link #getName()}
     */
    public CommandList(final String name, final String errorMessage) {
        this(name, errorMessage, new int[] {
            IStatus.OK
        });
    }

    /**
     * <p>
     * Creates a new {@link CommandList}, which is initially empty. Commands are
     * then added by calling {@link #addCommand(ICommand)}.
     * </p>
     *
     * <p>
     * The <code>continableSeverities</code> parameter is used to determine when
     * to stop running of the {@link CommandList}. If running a subcommand
     * produces an {@link IStatus} that has a severity not in the continuable
     * severities, a {@link CoreException} will be thrown instead of continuing
     * to run subcommands.
     * </p>
     *
     * @param name
     * @param continuableSeverities
     */
    public CommandList(final String name, final String errorMessage, final int[] continuableSeverities) {
        this.name = name;
        errorDescription = errorMessage;

        setCancellable(true);
        this.continuableSeverities = continuableSeverities;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getErrorDescription() {
        return errorDescription;
    }

    @Override
    public String getLoggingDescription() {
        return null;
    }

    @Override
    public void setCancellable(final boolean cancelable) {
        super.setCancellable(cancelable);
    }

    /**
     * Adds a subcommand to this {@link CommandList}. Subcommands are run in the
     * order they are added.
     *
     * @param command
     *        an {@link ICommand} to add (must not be <code>null</code>)
     * @return this {@link CommandList} instance (for method chaining)
     */
    public CommandList addCommand(final ICommand command) {
        commandList.add(command);
        return this;
    }

    public void setRollback(final boolean rollback) {
        this.rollback = rollback;
    }

    public boolean getRollback() {
        return rollback;
    }

    /*
     * (non-Javadoc)
     *
     * @seecom.microsoft.tfs.client.common.ui.shared.command.Command#doRun(org.
     * eclipse.core.runtime. IProgressMonitor)
     */
    @Override
    protected IStatus doRun(final IProgressMonitor progressMonitor) throws Exception {
        final ICommand[] commands = (ICommand[]) commandList.toArray(new ICommand[commandList.size()]);

        final MultiCommandHelper helper = new MultiCommandHelper(progressMonitor, continuableSeverities);
        helper.setRollback(rollback);
        helper.beginMainTask(getName(), commands.length);

        for (int i = 0; i < commands.length; i++) {
            try {
                helper.runSubCommand(commands[i], 1, isCancellable(), null);
            } catch (final CoreException e) {
                /*
                 * MultiCommandHelper throws CoreException when processing must
                 * stop due to failure.
                 */
                break;
            }
        }

        return helper.getStatus(getErrorDescription());
    }
}
