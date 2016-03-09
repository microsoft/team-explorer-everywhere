// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.command;

import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.wizard.IWizardContainer;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.framework.command.ICommand;

/**
 * <p>
 * A subclass of {@link RunnableContextCommandExecutor} that is passed an
 * {@link IWizardContainer} at construction. This executor executes
 * {@link ICommand}s in the context of a wizard.
 * </p>
 *
 * <p>
 * In addition, this executor will raise an {@link ErrorDialog} on the
 * {@link IWizardContainer}'s {@link Shell} if the status when a command is
 * finished meets certain criteria, as determined by
 * {@link ErrorDialogCommandFinishedCallback}.
 * </p>
 *
 * @see RunnableContextCommandExecutor
 * @see IWizardContainer
 * @see ErrorDialogCommandFinishedCallback
 */
public class WizardContainerCommandExecutor extends RunnableContextCommandExecutor {
    /**
     * Creates a new {@link WizardContainerCommandExecutor} that uses the
     * specified {@link IWizardContainer}.
     *
     * @param wizardContainer
     *        an {@link IWizardContainer} to use (must not be <code>null</code>)
     */
    public WizardContainerCommandExecutor(final IWizardContainer wizardContainer) {
        super(wizardContainer.getShell(), wizardContainer);
    }
}
