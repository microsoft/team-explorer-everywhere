// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.runnable;

import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.ProgressMonitorWrapper;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;

import com.microsoft.tfs.client.common.ui.framework.helper.ShellUtils;
import com.microsoft.tfs.client.common.ui.framework.helper.UIHelpers;
import com.microsoft.tfs.util.Check;

/**
 * <p>
 * An implementation of IRunnableContext that uses a ProgressMonitorDialog.
 * </p>
 * <p>
 * However, unlike using ProgressMonitorDialog directly, using this
 * IRunnableContext defers the display of the progress monitor dialog for some
 * set amount of time. If the underlying IRunnableWithProgress has completed
 * before the defer time is up, then no progress monitor will be displayed.
 * Using this IRunnableContext helps reduce flicker when IRunnableWithProgress
 * instances are normally short-lived.
 * </p>
 */
public class DeferredProgressMonitorDialogContext implements IRunnableContext {
    private static final Log log = LogFactory.getLog(DeferredProgressMonitorDialogContext.class);

    /*
     * This class is modeled after what happens during an
     * IProgressService.busyCursorWhile() call.
     */

    private final Shell shell;
    private final long deferTimeMillis;

    private ProgressMonitorDialog dialog;

    public DeferredProgressMonitorDialogContext(final Shell shell, final long deferTimeMillis) {
        Check.notNull(shell, "shell"); //$NON-NLS-1$

        this.shell = shell;
        this.deferTimeMillis = deferTimeMillis;
    }

    public void setCancelable(final boolean cancelable) {
        if (dialog != null) {
            dialog.setCancelable(cancelable);
        }
    }

    @Override
    public void run(final boolean fork, final boolean cancelable, final IRunnableWithProgress inputRunnable)
        throws InvocationTargetException,
            InterruptedException {
        final RunnableWithProgressWrapper wrappedRunnable = new RunnableWithProgressWrapper(inputRunnable);

        final InvocationTargetException[] invocationTargetExceptionHolder = new InvocationTargetException[1];
        final InterruptedException[] interruptedExceptionHolder = new InterruptedException[1];

        /*
         * Start executing the command in the context of a
         * ProgressMonitorDialog, however do not actually open the progress
         * monitor dialog. The command will execute properly, and we can open
         * the progress monitor dialog after our defer time.
         */
        UIHelpers.runOnUIThread(shell, false, new Runnable() {
            @Override
            public void run() {
                dialog = new ProgressMonitorDialog(shell);
                dialog.setOpenOnRun(false);

                /*
                 * Create a job that will open the progress monitor dialog after
                 * our defer time has elapsed.
                 */
                final Job showProgressUIJob = createProgressUIJob(inputRunnable, wrappedRunnable, dialog);

                showProgressUIJob.setSystem(true);
                showProgressUIJob.schedule(deferTimeMillis);

                BusyIndicator.showWhile(Display.getCurrent(), new Runnable() {
                    @Override
                    public void run() {
                        setUserInterfaceActive(false);
                        try {
                            /* Run the command. */
                            dialog.run(fork, cancelable, wrappedRunnable);
                        } catch (final InvocationTargetException e) {
                            invocationTargetExceptionHolder[0] = e;
                        } catch (final InterruptedException e) {
                            interruptedExceptionHolder[0] = e;
                        } finally {
                            setUserInterfaceActive(true);
                        }
                    }
                });
            }
        });

        if (invocationTargetExceptionHolder[0] != null) {
            throw invocationTargetExceptionHolder[0];
        }

        if (interruptedExceptionHolder[0] != null) {
            throw interruptedExceptionHolder[0];
        }
    }

    private Job createProgressUIJob(
        final IRunnableWithProgress inputRunnable,
        final RunnableWithProgressWrapper wrappedRunnable,
        final ProgressMonitorDialog dialog) {
        return new Job("") //$NON-NLS-1$
        {
            @Override
            protected IStatus run(final IProgressMonitor monitor) {
                Display.getDefault().asyncExec(new Runnable() {
                    @Override
                    public void run() {
                        setUserInterfaceActive(true);

                        /*
                         * Note that this job is executed regardless of whether
                         * the command is still executing. If it has finished,
                         * the progress monitor dialog will be disposed (and
                         * getShell will return null.)
                         */
                        if (dialog.getShell() != null) {
                            /*
                             * Attempt to avoid a UI hang problem. If there is
                             * already a modal Shell, opening the progress
                             * monitor will result in multiple modal Shells.
                             * Note that this cannot be avoided with a
                             * parent-child relationship on all window managers.
                             *
                             * If this occurs, reschedule this deferred runnable
                             * for another interval of the deferTimeMillis. The
                             * blocking dialog may have been removed in this
                             * time (eg, in the case of an authentication
                             * dialog.)
                             */
                            final Shell blocker = ShellUtils.getModalBlockingShell(dialog.getShell());
                            if (blocker != null) {
                                final String messageFormat =
                                    "unsafe modal operation - shell [{0} ({1})] for [{2}] blocked by shell [{3} ({4})]"; //$NON-NLS-1$
                                final String message = MessageFormat.format(
                                    messageFormat,
                                    dialog.getShell(),
                                    Integer.toHexString(System.identityHashCode(dialog.getShell())),
                                    inputRunnable,
                                    blocker,
                                    Integer.toHexString(System.identityHashCode(blocker)));

                                log.info(message);

                                /* Requeue the dialog */
                                final Job showProgressUIJob =
                                    createProgressUIJob(inputRunnable, wrappedRunnable, dialog);

                                showProgressUIJob.setSystem(true);
                                showProgressUIJob.schedule(deferTimeMillis);

                                return;
                            }
                        }

                        if (dialog.getShell() != null) {
                            if (log.isTraceEnabled()) {
                                final String messageFormat = "opening shell [{0} ({1})] for [{2}]"; //$NON-NLS-1$
                                final String message = MessageFormat.format(
                                    messageFormat,
                                    dialog.getShell(),
                                    Integer.toHexString(System.identityHashCode(dialog.getShell())),
                                    inputRunnable);

                                log.trace(message);
                            }
                        }

                        /*
                         * It is safe to call dialog.open() even when the
                         * command has finished executing.
                         */
                        dialog.open();

                        /*
                         * this line works around a problem with progress
                         * monitor dialog: updates to the task name are not
                         * always displayed if openOnRun is false and the
                         * updates are made before the dialog is opened
                         *
                         * the fix is to cache changes made to the task name
                         * (through the interface wrapper classes defined below)
                         * and re-set the task name to the same value at a same
                         * time (after the dialog has opened)
                         */
                        wrappedRunnable.getCustomProgressMonitor().updateTaskNameIfSet();
                    }
                });
                return Status.OK_STATUS;
            }
        };
    }

    private static void setUserInterfaceActive(final boolean active) {
        final IWorkbench workbench = PlatformUI.getWorkbench();
        final Shell[] shells = workbench.getDisplay().getShells();
        for (final Shell shell : shells) {
            if (!shell.isDisposed()) {
                shell.setEnabled(active);
            }
        }
    }

    /**
     * An {@link IRunnableWithProgress} implementation that wraps a real
     * {@link IRunnableWithProgress} and allows for hooking of the
     * {@link IProgressMonitor} passed in the {@link #run(IProgressMonitor)}
     * method.
     */
    private static class RunnableWithProgressWrapper implements IRunnableWithProgress {
        private final IRunnableWithProgress wrappedRunnable;
        private CustomProgressMonitor customProgressMonitor;

        public RunnableWithProgressWrapper(final IRunnableWithProgress wrappedRunnable) {
            this.wrappedRunnable = wrappedRunnable;
        }

        @Override
        public void run(final IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
            customProgressMonitor = new CustomProgressMonitor(monitor);
            wrappedRunnable.run(customProgressMonitor);
        }

        public CustomProgressMonitor getCustomProgressMonitor() {
            return customProgressMonitor;
        }
    }

    /**
     * <p>
     * An {@link IProgressMonitor} implementation that wraps another
     * {@link IProgressMonitor} and captures the task name passed in
     * {@link IProgressMonitor#beginTask(String, int)} and
     * {@link IProgressMonitor#setTaskName(String)}. The captured task name can
     * be set on the wrapped {@link IProgressMonitor} at a later point in time
     * by calling {@link #updateTaskNameIfSet()}.
     * </p>
     *
     * <p>
     * This works around a limitation of {@link IProgressMonitor}s and
     * {@link ProgressMonitorDialog}: see above for more.
     * </p>
     */
    private static class CustomProgressMonitor extends ProgressMonitorWrapper {
        private String taskName;

        public CustomProgressMonitor(final IProgressMonitor wrappedMonitor) {
            super(wrappedMonitor);
        }

        @Override
        public void beginTask(final String name, final int totalWork) {
            taskName = name;
            super.beginTask(name, totalWork);
        }

        @Override
        public void setTaskName(final String name) {
            taskName = name;
            super.setTaskName(name);
        }

        public void updateTaskNameIfSet() {
            if (taskName != null) {
                getWrappedProgressMonitor().setTaskName(taskName);
            }
        }
    }
}
