// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.framework.command;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import com.microsoft.tfs.client.common.TFSCommonClientPlugin;

/**
 * <p>
 * This class implements a non-blocking {@link ICommandExecutor} by making use
 * of the Eclipse {@link Job} framework.
 * </p>
 *
 * <p>
 * The {@link ICommandExecutor#execute(ICommand)} method is implemented by
 * scheduling a new {@link Job} to run the specified {@link ICommand} and then
 * returning immediately.
 * </p>
 *
 * <p>
 * As a non-blocking {@link ICommandExecutor}, this executor returns
 * <code>true</code> from {@link #isAsync()} and returns a {@link FutureStatus}
 * from the {@link #execute(ICommand)} method. The {@link FutureStatus} returned
 * by this executor returns the {@link Job} produced by executor from the
 * {@link FutureStatus#getAsyncObject()} method.
 * </p>
 *
 * <p>
 * Attributes of the {@link Job}s created and scheduled by this executor can be
 * configured by passing an instance of {@link JobOptions} at construction time.
 * The no-args constructor uses default values for all of the configurable
 * {@link Job} attributes - see the {@link JobOptions} class for details.
 * </p>
 *
 * @see ICommandExecutor
 * @see Job
 * @see FutureStatus
 * @see JobOptions
 */
public class JobCommandExecutor extends CommandExecutor {
    private static final Log log = LogFactory.getLog(JobCommandExecutor.class);

    private final JobOptions jobOptions;

    /**
     * Creates a new {@link JobCommandExecutor} that creates {@link Job}s
     * configured with default attributes. For control over some of the
     * {@link Job} attributes, use the {@link #JobCommandExecutor(JobOptions)}
     * constructor instead.
     */
    public JobCommandExecutor() {
        this(null);
    }

    /**
     * <p>
     * Creates a new {@link JobCommandExecutor}. {@link Job}s created and
     * scheduled by this executor will be configured with the attribute values
     * that are set in the specified {@link JobOptions} instance.
     * </p>
     * <p>
     * Note that this executor does not hold a reference to the specified
     * {@link JobOptions} after this constructor returns - changes to the
     * {@link JobOptions} instance made after passing it to this constructor
     * will not be reflected in this {@link JobCommandExecutor}.
     * </p>
     *
     * @param jobOptions
     *        holds attribute values that are used to configure {@link Job}s
     *        created by this {@link JobCommandExecutor} (pass <code>null</code>
     *        to use default values for all configurable attributes)
     *
     */
    public JobCommandExecutor(final JobOptions jobOptions) {
        this.jobOptions = new JobOptions(jobOptions);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.microsoft.tfs.client.common.ui.shared.command.CommandExecutor#isAsync
     * ()
     */
    @Override
    public boolean isAsync() {
        return true;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.microsoft.tfs.client.common.ui.shared.command.CommandExecutor#execute
     * (com.microsoft.tfs.client.command.ICommand)
     */
    @Override
    public IStatus execute(final ICommand command) {
        final Job job = jobOptions.createJobFor(command, getCommandStartedCallback(), getCommandFinishedCallback());

        jobOptions.configure(job);

        jobOptions.schedule(job);

        return new JobFutureStatus(job);
    }

    /**
     * A subclass of {@link AbstractFutureStatus} that implements a
     * {@link FutureStatus} based around using a {@link Job} as the async
     * object.
     * <p>
     * Delegates {@link #join()} duties to the
     * {@link ExtensionPointAsyncObjectWaiter} to give UI plug-ins a chance to
     * keep UI events going.
     */
    protected static class JobFutureStatus extends AbstractFutureStatus {
        protected final Job job;

        private IStatus jobResult;
        private final Object jobResultLock = new Object();

        public JobFutureStatus(final Job job) {
            super(job);
            this.job = job;
        }

        @Override
        public boolean isCompleted() {
            return (job.getState() == Job.NONE && job.getResult() != null);
        }

        @Override
        public final void join() {
            try {
                // Use the implementation that can defer to extensions
                new ExtensionPointAsyncObjectWaiter().joinJob(job);

                if (job.getResult() == null) {
                    /* Build a dummy exception for a stack trace */
                    log.error("Unexpected null job result while waiting for job to complete", new Exception()); //$NON-NLS-1$

                    jobResult =
                        new Status(Status.ERROR, TFSCommonClientPlugin.PLUGIN_ID, 0, "Unexpected null jobresult", null); //$NON-NLS-1$
                }
            } catch (final InterruptedException e) {
                synchronized (jobResultLock) {
                    jobResult = new Status(Status.ERROR, TFSCommonClientPlugin.PLUGIN_ID, 0, null, e);
                }
            }
        }

        @Override
        protected IStatus getCompletedStatus() {
            synchronized (jobResultLock) {
                if (jobResult == null) {
                    jobResult = job.getResult();
                }

                return jobResult;
            }
        }
    }
}
