// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.command;

import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.framework.command.ICommand;
import com.microsoft.tfs.util.Check;

/**
 * A concrete subclass of {@link AbstractRunnableContextCommandExecutor} that is
 * passed an {@link IRunnableContext} at construction. This runnable context is
 * then used for all command executions.
 *
 * @see AbstractRunnableContextCommandExecutor
 */
public class RunnableContextCommandExecutor extends AbstractRunnableContextCommandExecutor {
    private final IRunnableContext runnableContext;

    /**
     * Creates a new instance of {@link RunnableContextCommandExecutor}. The
     * specified {@link IRunnableContext} will be used for all command
     * executions.
     *
     * @param runnableContext
     *        an {@link IRunnableContext} to use (must not be <code>null</code>)
     */
    public RunnableContextCommandExecutor(final Shell shell, final IRunnableContext runnableContext) {
        super(shell);

        Check.notNull(runnableContext, "runnableContext"); //$NON-NLS-1$

        this.runnableContext = runnableContext;
    }

    /*
     * (non-Javadoc)
     *
     * @seecom.microsoft.tfs.client.common.ui.shared.command.
     * AbstractRunnableContextCommandExecutor
     * #getRunnableContext(com.microsoft.tfs
     * .client.common.ui.shared.command.ICommand)
     */
    @Override
    protected IRunnableContext getRunnableContext(final ICommand command) {
        return runnableContext;
    }
}
