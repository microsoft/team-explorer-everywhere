// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.ui.filemodification;

import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.dialogs.vc.CheckoutDialog;
import com.microsoft.tfs.client.common.ui.prefs.UIPreferenceConstants;
import com.microsoft.tfs.client.common.vc.TypedItemSpec;
import com.microsoft.tfs.client.eclipse.filemodification.TFSFileModificationOptions;
import com.microsoft.tfs.client.eclipse.filemodification.TFSFileModificationOptionsProvider;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.LockLevel;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.util.Check;

public class TFSFileModificationUIOptionsProvider implements TFSFileModificationOptionsProvider {
    private final Shell shell;

    public TFSFileModificationUIOptionsProvider(final Shell shell) {
        Check.notNull(shell, "shell"); //$NON-NLS-1$

        this.shell = shell;
    }

    @Override
    public TFSFileModificationOptions getOptions(String[] serverPaths, final LockLevel forcedLockLevel) {
        final LockLevel defaultLockLevel = CheckoutDialog.getDefaultLockLevel();

        final IPreferenceStore preferenceStore = TFSCommonUIClientPlugin.getDefault().getPreferenceStore();

        final boolean isSynchronous = preferenceStore.getBoolean(UIPreferenceConstants.CHECKOUT_SYNCHRONOUS);
        final boolean isForeground = preferenceStore.getBoolean(UIPreferenceConstants.CHECKOUT_FOREGROUND);
        final boolean isGetLatest = preferenceStore.getBoolean(UIPreferenceConstants.GET_LATEST_ON_CHECKOUT);

        if (!preferenceStore.getBoolean(UIPreferenceConstants.PROMPT_BEFORE_CHECKOUT)) {
            final LockLevel lockLevel = forcedLockLevel != null ? forcedLockLevel : defaultLockLevel;

            return new TFSFileModificationOptions(
                Status.OK_STATUS,
                serverPaths,
                lockLevel,
                isSynchronous,
                isForeground,
                isGetLatest);
        }

        TypedItemSpec[] serverItems = new TypedItemSpec[serverPaths.length];

        for (int i = 0; i < serverPaths.length; i++) {
            serverItems[i] = new TypedItemSpec(serverPaths[i], RecursionType.NONE, ItemType.FILE);
        }

        final CheckoutDialogRunnable dialogRunnable = new CheckoutDialogRunnable(shell, serverItems, forcedLockLevel);

        shell.getDisplay().syncExec(new Runnable() {
            @Override
            public void run() {
                dialogRunnable.run();
            }
        });

        if (dialogRunnable.getResult() != IDialogConstants.OK_ID) {
            return new TFSFileModificationOptions(Status.CANCEL_STATUS);
        }

        serverItems = dialogRunnable.getItemSpecs();
        final LockLevel lockLevel = dialogRunnable.getLockLevel();

        serverPaths = new String[serverItems.length];

        for (int i = 0; i < serverItems.length; i++) {
            serverPaths[i] = serverItems[i].getItem();
        }

        return new TFSFileModificationOptions(Status.OK_STATUS, serverPaths, lockLevel, true, true, isGetLatest);
    }

    private static final class CheckoutDialogRunnable implements Runnable {
        /* Synchronize for visibility */
        private final Object lock = new Object();

        private final Shell shell;
        private TypedItemSpec[] itemSpecs;
        private LockLevel lockLevel;
        private int result;

        public CheckoutDialogRunnable(final Shell shell, final TypedItemSpec[] itemSpecs, final LockLevel lockLevel) {
            this.shell = shell;
            this.itemSpecs = itemSpecs;
            this.lockLevel = lockLevel;
        }

        @Override
        public void run() {
            synchronized (lock) {
                final CheckoutDialog checkoutDialog = new CheckoutDialog(shell, itemSpecs, lockLevel);

                if ((result = checkoutDialog.open()) != IDialogConstants.OK_ID) {
                    return;
                }

                itemSpecs = checkoutDialog.getCheckedTypedItemSpecs();
                lockLevel = checkoutDialog.getLockLevel();
            }
        }

        public int getResult() {
            synchronized (lock) {
                return result;
            }
        }

        public TypedItemSpec[] getItemSpecs() {
            synchronized (lock) {
                return itemSpecs;
            }
        }

        public LockLevel getLockLevel() {
            synchronized (lock) {
                return lockLevel;
            }
        }
    }
}
