// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.framework.command;

/**
 * <p>
 * An {@link ICommandStartedCallback} is used to perform some processing before
 * an {@link ICommand} has started being executed.
 * </p>
 *
 * <p>
 * For example, an {@link ICommandStartedCallback} could log that the command is
 * about to execute.
 * </p>
 */
public interface ICommandStartedCallback {
    /**
     * Called before a command has started being executed.
     *
     * @param command
     *        the {@link ICommand} that has finished executing
     */
    public void onCommandStarted(ICommand command);
}
