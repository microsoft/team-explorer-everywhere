// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.framework.command;

import org.eclipse.core.runtime.jobs.Job;

/**
 * <p>
 * An {@link ICommandJobFactory} is used by the {@link JobCommandExecutor} to
 * create new {@link Job} instances for {@link ICommand}s.
 * </p>
 */
public interface ICommandJobFactory {
    /**
     * Called to create a new {@link Job} instance for the specified
     * {@link ICommand} and {@link ICommandFinishedCallback}. The returned
     * {@link Job} should run the {@link ICommand} when the job is run, and it
     * should invoke the {@link ICommandFinishedCallback} after the
     * {@link ICommand} has completed.
     *
     * @param command
     *        an {@link ICommand} (must not be <code>null</code>)
     * @param commandStartedCallback
     *        an {@link ICommandStartedCallback} (may be <code>null</code>)
     * @param commandFinishedCallback
     *        an {@link ICommandFinishedCallback} (may be <code>null</code>)
     * @return a new {@link Job} instance (must not be <code>null</code>)
     */
    public Job newJobFor(
        ICommand command,
        ICommandStartedCallback commandStartedCallback,
        ICommandFinishedCallback commandFinishedCallback);
}
