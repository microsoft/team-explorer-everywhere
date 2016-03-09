// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.dialogs.vc;

import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.vc.TypedItemSpec;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.LockLevel;

public class LockDialog extends CheckoutDialog {
    public LockDialog(final Shell shell, final TypedItemSpec[] itemSpecs) {
        super(shell, itemSpecs);
    }

    public LockDialog(final Shell shell, final TypedItemSpec[] itemSpecs, final LockLevel lockLevel) {
        super(shell, itemSpecs, lockLevel);
    }

    @Override
    protected String provideDialogTitle() {
        return Messages.getString("LockDialog.DialogTitle"); //$NON-NLS-1$
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected LockLevelComboItem[] getEnabledLockLevelItems() {
        return new LockLevelComboItem[] {
            CheckoutDialog.LOCK_LEVEL_CHECKOUT,
            CheckoutDialog.LOCK_LEVEL_CHECKIN
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected LockLevelComboItem getDefaultLockLevelItem() {
        return CheckoutDialog.LOCK_LEVEL_CHECKOUT;
    }
}
