// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.framework.command;

import org.eclipse.core.runtime.IProgressMonitor;

import com.microsoft.tfs.util.Check;

/**
 * <p>
 * {@link CommandFactory} provides access to command operations that create new
 * {@link ICommand} instances. These operations involve using "specialty"
 * {@link ICommand} implementations that are provided by the command framework.
 * </p>
 *
 * <p>
 * There's nothing wrong with using specialty {@link ICommand} implementations
 * directly instead of performing the equivalent operation through this factory.
 * However, by using the factory, you don't need to remember the individual
 * implementation class names. In addition, using this factory provides a layer
 * of indirection that means less code has to change as improved specialty
 * {@link ICommand} implementations are written. Also, clients should not cast
 * the {@link ICommand}s returned by this factory to a concrete implementation.
 * The implementations returned by this factory are not guaranteed to remain the
 * same.
 * </p>
 *
 * @see ICommand
 */
public class CommandFactory {
    /**
     * <p>
     * Creates a new {@link ICommand} that is always cancelable and wraps the
     * specified {@link ICommand}. <b>Use this method with caution</b>: the
     * returned {@link ICommand} executes the specified {@link ICommand} in a
     * background thread. As of now, the spawning of new background threads is
     * not metered in any way.
     * </p>
     *
     * <p>
     * This functionality is intended for commands that can't easily poll for
     * cancelation themselves. For instance, a command that calls a blocking
     * method that could potentially block for many seconds has no way to check
     * for cancelation using {@link IProgressMonitor}'s polling mechanism. The
     * {@link ICommand} returned by this method will poll for cancelation in one
     * thread while the wrapped command executes in another.
     * </p>
     *
     * @param command
     *        the actual {@link ICommand} to wrap (must not be <code>null</code>
     *        )
     * @return a new {@link ICommand} as described above
     */
    public static ICommand newCancelableCommand(final ICommand command) {
        return new ThreadedCancellableCommand(command);
    }

    /**
     * Combines multiple {@link ICommand}s into a single command.
     *
     * @param name
     *        the name that the new combined command will return from
     *        {@link ICommand#getName()} (must not be <code>null</code>)
     * @param commands
     *        the array of {@link ICommand}s to combine (must not be
     *        <code>null</code> and must not have any <code>null</code>
     *        elements)
     * @return a new {@link ICommand} as described above
     */
    public static ICommand newCombinedCommand(final String name, final String errorMessage, final ICommand[] commands) {
        Check.notNull(commands, "commands"); //$NON-NLS-1$

        final CommandList commandList = new CommandList(name, errorMessage);

        for (int i = 0; i < commands.length; i++) {
            if (commands[i] == null) {
                throw new IllegalArgumentException("element " + i + " is  null"); //$NON-NLS-1$ //$NON-NLS-2$
            }

            commandList.addCommand(commands[i]);
        }

        return commandList;
    }
}
