// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.command;

import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.wizard.IWizardContainer;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.framework.command.ICommandExecutor;
import com.microsoft.tfs.client.common.framework.command.JobOptions;
import com.microsoft.tfs.client.common.framework.command.ThreadCommandExecutor;

/**
 * <p>
 * {@link UICommandExecutorFactory} provides access to the most commonly used
 * {@link ICommandExecutor} implementations. To get an {@link ICommandExecutor}
 * from this factory call one of the <code>new*</code> methods, passing the
 * appropriate parameters (if any).
 * </p>
 *
 * <p>
 * There's nothing wrong with using the {@link ICommandExecutor} implementations
 * directly instead of using this factory. However, by using the factory, you
 * don't need to remember the individual implementation class names and you can
 * use code completion in the IDE to help choose an appropriate executor. In
 * addition, using this factory provides a layer of indirection that means less
 * code has to change as improved {@link ICommandExecutor} implementations are
 * written. Also, clients should not cast the {@link ICommandExecutor}s returned
 * by this factory to a concrete implementation. The implementations returned by
 * this factory are not guaranteed to remain the same.
 * </p>
 *
 * @see ICommandExecutor
 */
public class UICommandExecutorFactory {
    /**
     * Creates a new asynchronous {@link ICommandExecutor} that makes use of the
     * Eclipse job framework and can process UI thread messages while waiting
     * for jobs to finish.
     *
     * @return a new {@link ICommandExecutor} as described above
     */
    public static ICommandExecutor newUIJobCommandExecutor(final Shell shell) {
        return new UIJobCommandExecutor(shell);
    }

    /**
     * <p>
     * Creates a new asynchronous {@link ICommandExecutor} that makes use of the
     * Eclipse job framework and can process UI thread messages while waiting
     * for jobs to. Certain attributes of the {@link Job}s that are created by
     * the executor can be controlled through the specified {@link JobOptions}
     * parameter.
     * </p>
     *
     * <p>
     * Note that no reference is held to the give {@link JobOptions} object
     * after this method returns. This means that any changes made to the
     * {@link JobOptions} after calling this method will not impact the returned
     * executor.
     * </p>
     *
     * @param jobOptions
     *        a {@link JobOptions} instance containing values used to configure
     *        new {@link Job}s, or <code>null</code> to use default
     *        configuration
     * @return a new {@link ICommandExecutor} as described above
     */
    public static ICommandExecutor newUIJobCommandExecutor(final Shell shell, final JobOptions jobOptions) {
        return new UIJobCommandExecutor(shell, jobOptions);
    }

    /**
     * Creates a new {@link ICommandExecutor} that is suitable for use in
     * general UI scenarios. The executor will execute commands invisibly for a
     * short period of time, and then show a modal dialog that reports on the
     * progress of the command and allows the user to cancel.
     *
     * @param shell
     *        a parent {@link Shell} (must not be <code>null</code>)
     * @return a new {@link ICommandExecutor} as described above
     */
    public static ICommandExecutor newUICommandExecutor(final Shell shell) {
        return new ProgressMonitorDialogCommandExecutor(shell);
    }

    /**
     * <p>
     * Creates a new {@link ICommandExecutor} that is suitable for use in
     * general UI scenarios. The executor will execute commands invisibly for a
     * short period of time, and then show a modal dialog that reports on the
     * progress of the command and allows the user to cancel.
     * </p>
     *
     * <p>
     * The amount of time that the command runs before showing visible progress
     * can be configured through the <Code>progressUIDeferTime</code> parameter.
     * If this parameter is <code>0</code>, progress will be shown for the
     * command immediately.
     * </p>
     *
     * @param shell
     *        a parent {@link Shell} (must not be <code>null</code>)
     * @param progressUIDeferTime
     *        amount of time in milliseconds to defer showing progress
     * @return a new {@link ICommandExecutor} as described above
     */
    public static ICommandExecutor newUICommandExecutor(final Shell shell, final long progressUIDeferTime) {
        return new ProgressMonitorDialogCommandExecutor(shell, progressUIDeferTime);
    }

    /**
     * Creates a new {@link ICommandExecutor} that can be used by a wizard or
     * wizard page.
     *
     * @param wizardContainer
     *        an {@link IWizardContainer} (must not be <code>null</code>)
     * @return a new {@link ICommandExecutor} as described above
     */
    public static ICommandExecutor newWizardCommandExecutor(final IWizardContainer wizardContainer) {
        return new WizardContainerCommandExecutor(wizardContainer);
    }

    /**
     * Creates a new {@link ICommandExecutor} that displays a busy indicator
     * while it runs commands.
     *
     * @param display
     *        the SWT {@link Display} (must not be <code>null</code>)
     * @return a new {@link ICommandExecutor} as described above
     */
    public static ICommandExecutor newBusyIndicatorCommandExecutor(final Shell shell) {
        return new BusyIndicatorCommandExecutor(shell);
    }

    /**
     * Creates a new asynchronous {@link ICommandExecutor} that uses background
     * threads to execute commands.
     *
     * @return a new {@link ICommandExecutor} as described above
     */
    public static ICommandExecutor newThreadCommandExecutor() {
        return new ThreadCommandExecutor();
    }
}
