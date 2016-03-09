// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.command;

import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.framework.command.ICommand;
import com.microsoft.tfs.client.common.framework.command.ICommandFinishedCallback;
import com.microsoft.tfs.client.common.framework.command.ICommandJobFactory;
import com.microsoft.tfs.client.common.framework.command.ICommandStartedCallback;
import com.microsoft.tfs.client.common.framework.command.JobCommandAdapter;
import com.microsoft.tfs.client.common.framework.command.JobCommandExecutor;
import com.microsoft.tfs.client.common.framework.command.JobOptions;
import com.microsoft.tfs.util.Check;

/**
 * A {@link JobCommandExecutor} that understands that Eclipse has a UI, and thus
 * can make use of displaying warnings, etc.
 * <p>
 * <em>
 * Always prefer {@link UIJobCommandExecutor} to {@link JobCommandExecutor} in a
 * graphical context.
 * </em>
 * <p>
 * An important feature of this extension to {@link JobCommandExecutor} is its
 * ability to process UI thread messages while waiting for a job to finish, when
 * the thread waiting on the job is the UI thread (see the implementation in
 * {@link UIJobFutureStatus} ). The base class, {@link JobCommandExecutor}, does
 * not offer this feature. It waits with a simple blocking {@link Job#join()}
 * which prevents the running {@link Job} from doing work on the UI thread (for
 * example, with {@link Display#syncExec(Runnable))}.
 */
public class UIJobCommandExecutor extends JobCommandExecutor {
    public UIJobCommandExecutor(final Shell shell) {
        this(shell, null);
    }

    public UIJobCommandExecutor(final Shell shell, final JobOptions jobOptions) {
        super(createJobOptions(jobOptions));

        Check.notNull(shell, "shell"); //$NON-NLS-1$
        setCommandFinishedCallback(UICommandFinishedCallbackFactory.getDefaultCallback(shell));
    }

    private static JobOptions createJobOptions(final JobOptions jobOptions) {
        final JobOptions newJobOptions = new JobOptions(jobOptions);
        newJobOptions.setCommandJobFactory(new UICommandJobFactory());

        return newJobOptions;
    }

    /**
     * A default implementation of {@link ICommandJobFactory}, which creates new
     * {@link JobCommandAdapter} instances to satisfy the
     * {@link #newJobFor(ICommand, ICommandFinishedCallback)} method.
     */
    private static class UICommandJobFactory implements ICommandJobFactory {
        /**
         * {@inheritDoc}
         */
        @Override
        public Job newJobFor(
            final ICommand command,
            final ICommandStartedCallback commandStartedCallback,
            final ICommandFinishedCallback commandFinishedCallback) {
            return new UIJobCommandAdapter(command, commandStartedCallback, commandFinishedCallback);
        }
    }
}
