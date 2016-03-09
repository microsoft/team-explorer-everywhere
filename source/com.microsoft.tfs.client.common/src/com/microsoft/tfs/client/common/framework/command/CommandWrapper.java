// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.framework.command;

/**
 * An interface used by command wrappers to expose the underlying wrapped
 * command.
 *
 * Commands that merely exist to provide execution environments (eg,
 * {@link ThreadedCancellableCommand}) should implement this interface for
 * improved logging.
 */
public interface CommandWrapper {
    /**
     * Provides the underlying command that is to be executed.
     *
     * @return The {@link ICommand} that is being wrapped (not <code>null</code>
     *         )
     */
    public ICommand getWrappedCommand();
}
