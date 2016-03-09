// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.tasks.vc;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.commands.vc.QueryExclusiveCheckoutCommand;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.dialogs.vc.CheckoutDialog;
import com.microsoft.tfs.client.common.vc.TypedItemSpec;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.LockLevel;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;
import com.microsoft.tfs.util.Check;

/**
 * Handles the check out for edit procedure for {@link ItemSpecs}s, including
 * raising the dialog and running the command
 *
 * @threadsafety unknown
 */
public class CheckoutWithPromptTask extends AbstractCheckoutTask {
    private final TypedItemSpec[] itemSpecs;

    /**
     * Creates a {@link CheckoutTask}.
     *
     * @param shell
     *        the shell (must not be <code>null</code>)
     * @param repository
     *        the repository (must not be <code>null</code>)
     * @param itemSpecs
     *        the items to check out (must not be <code>null</code>)
     */
    public CheckoutWithPromptTask(final Shell shell, final TFSRepository repository, final TypedItemSpec[] itemSpecs) {
        super(shell, repository);

        Check.notNull(itemSpecs, "itemSpecs"); //$NON-NLS-1$

        this.itemSpecs = itemSpecs;
    }

    @Override
    public IStatus run() {
        /* Query exclusive checkout annotation */
        final QueryExclusiveCheckoutCommand queryExclusiveCheckoutCommand =
            new QueryExclusiveCheckoutCommand(getRepository(), itemSpecs);

        final IStatus queryStatus = getCommandExecutor().execute(queryExclusiveCheckoutCommand);

        if (!queryStatus.isOK()) {
            return queryStatus;
        }

        final CheckoutDialog checkoutDialog = queryExclusiveCheckoutCommand.isExclusiveCheckout()
            ? new CheckoutDialog(getShell(), itemSpecs, LockLevel.CHECKOUT) : new CheckoutDialog(getShell(), itemSpecs);

        if (checkoutDialog.open() != IDialogConstants.OK_ID) {
            return Status.OK_STATUS;
        }

        final ItemSpec[] selectedItemSpecs = checkoutDialog.getCheckedTypedItemSpecs();

        if (selectedItemSpecs.length == 0) {
            return Status.OK_STATUS;
        }

        return checkout(selectedItemSpecs, checkoutDialog.getLockLevel(), null);
    }
}