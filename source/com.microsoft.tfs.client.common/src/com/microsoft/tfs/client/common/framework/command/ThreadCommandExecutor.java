// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.framework.command;

import java.text.MessageFormat;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.microsoft.tfs.client.common.TFSCommonClientPlugin;

/**
 * <p>
 * This class implements a non-blocking {@link ICommandExecutor} that uses
 * {@link Thread}s to execute commands.
 * </p>
 *
 * <p>
 * The {@link ICommandExecutor#execute(ICommand)} method is implementing by
 * starting a new {@link Thread} to run the specified {@link ICommand} and then
 * returning immediately. This executor makes use of the
 * {@link RunnableCommandAdapter} adapter class to adapt an {@link ICommand} to
 * a {@link Runnable}.
 * </p>
 *
 * <p>
 * As a non-blocking {@link ICommandExecutor}, this executor returns
 * <code>true</code> from {@link #isAsync()} and returns a {@link FutureStatus}
 * from the {@link #execute(ICommand)} method. The {@link FutureStatus} returned
 * by this executor returns the {@link Thread} produced by the executor from the
 * {@link FutureStatus#getAsyncObject()} method.
 * </p>
 *
 * @see ICommandExecutor
 * @see Thread
 * @see RunnableCommandAdapter
 * @see FutureStatus
 */
public class ThreadCommandExecutor extends CommandExecutor {
    /*
     * (non-Javadoc)
     *
     * @see
     * com.microsoft.tfs.client.common.ui.shared.command.CommandExecutor#execute
     * (com.microsoft.tfs.client.common.command.ICommand)
     */
    @Override
    public IStatus execute(final ICommand command) {
        final RunnableCommandAdapter adapter =
            new RunnableCommandAdapter(command, null, getCommandStartedCallback(), getCommandFinishedCallback());

        final String messageFormat = "ThreadCommandExecutor thread for [{0}]"; //$NON-NLS-1$
        final String message = MessageFormat.format(messageFormat, command.getName());
        final Thread thread = new Thread(adapter, message);
        thread.start();

        return new ThreadFutureStatus(thread, adapter);
    }

    /**
     * A subclass of {@link AbstractFutureStatus} that implements a
     * {@link FutureStatus} based around using a {@link Thread} as the async
     * object. It also tracks a {@link RunnableCommandAdapter} so the
     * {@link IStatus} produced by running a command in the thread can be
     * obtained.
     * <p>
     * Delegates {@link #join()} duties to the
     * {@link ExtensionPointAsyncObjectWaiter} to give UI plug-ins a chance to
     * keep UI events going.
     */
    private static class ThreadFutureStatus extends AbstractFutureStatus {
        private final Thread thread;
        private final RunnableCommandAdapter adapter;

        private IStatus status;
        private final Object statusLock = new Object();

        public ThreadFutureStatus(final Thread thread, final RunnableCommandAdapter adapter) {
            super(thread);
            this.thread = thread;
            this.adapter = adapter;
        }

        @Override
        public boolean isCompleted() {
            return !thread.isAlive();
        }

        @Override
        public void join() {
            try {
                // Use the implementation that can defer to extensions
                new ExtensionPointAsyncObjectWaiter().joinThread(thread);
            } catch (final InterruptedException e) {
                synchronized (statusLock) {
                    status = new Status(IStatus.ERROR, TFSCommonClientPlugin.PLUGIN_ID, 0, null, e);
                }
            }
        }

        @Override
        protected IStatus getCompletedStatus() {
            synchronized (statusLock) {
                if (status == null) {
                    status = adapter.getStatus();
                }

                return status;
            }
        }
    }
}
