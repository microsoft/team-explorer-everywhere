// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.tasks.vc;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.commands.vc.LockCommand;
import com.microsoft.tfs.client.common.commands.vc.QueryExclusiveCheckoutCommand;
import com.microsoft.tfs.client.common.framework.resources.command.ResourceChangingCommand;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.dialogs.vc.LockDialog;
import com.microsoft.tfs.client.common.ui.tasks.BaseTask;
import com.microsoft.tfs.client.common.vc.TypedItemSpec;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.LockLevel;
import com.microsoft.tfs.util.Check;

/**
 * Locks {@link ItemSpecs}s, including raising the dialog and running the
 * command.
 *
 * @threadsafety unknown
 */
public class LockTask extends BaseTask {
    private final TFSRepository repository;
    private final TypedItemSpec[] itemSpecs;

    /**
     * Creates a {@link LockTask}.
     *
     * @param shell
     *        the shell (must not be <code>null</code>)
     * @param repository
     *        the repository (must not be <code>null</code>)
     * @param itemSpecs
     *        the items to lock (must not be <code>null</code>)
     */
    public LockTask(final Shell shell, final TFSRepository repository, final TypedItemSpec[] itemSpecs) {
        super(shell);

        Check.notNull(repository, "repository"); //$NON-NLS-1$
        Check.notNull(itemSpecs, "itemSpecs"); //$NON-NLS-1$

        this.repository = repository;
        this.itemSpecs = itemSpecs;
    }

    @Override
    public IStatus run() {
        /* Query exclusive checkout annotation */
        final QueryExclusiveCheckoutCommand queryExclusiveCheckoutCommand =
            new QueryExclusiveCheckoutCommand(repository, itemSpecs);

        final IStatus queryStatus = getCommandExecutor().execute(queryExclusiveCheckoutCommand);

        if (!queryStatus.isOK()) {
            return queryStatus;
        }

        final LockDialog lockDialog = queryExclusiveCheckoutCommand.isExclusiveCheckout()
            ? new LockDialog(getShell(), itemSpecs, LockLevel.CHECKOUT) : new LockDialog(getShell(), itemSpecs);

        if (lockDialog.open() != IDialogConstants.OK_ID) {
            return Status.OK_STATUS;
        }

        final TypedItemSpec[] selectedItemSpecs = lockDialog.getCheckedTypedItemSpecs();

        if (selectedItemSpecs.length == 0) {
            return Status.OK_STATUS;
        }

        final LockCommand command = new LockCommand(repository, selectedItemSpecs, lockDialog.getLockLevel());

        return getCommandExecutor().execute(new ResourceChangingCommand(command));
    }
}
