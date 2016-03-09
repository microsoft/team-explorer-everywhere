// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.tasks.vc;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.dialogs.vc.UnshelveDialog;
import com.microsoft.tfs.client.common.ui.prefs.UIPreferenceConstants;
import com.microsoft.tfs.util.Check;

public class UnshelveTask extends AbstractUnshelveTask {
    private final TFSRepository repository;

    public UnshelveTask(final Shell shell, final TFSRepository repository) {
        super(shell, repository);

        Check.notNull(repository, "repository"); //$NON-NLS-1$

        this.repository = repository;
    }

    @Override
    public IStatus run() {
        final UnshelveDialog unshelveDialog = new UnshelveDialog(getShell(), repository);

        if (unshelveDialog.open() != IDialogConstants.OK_ID) {
            return Status.CANCEL_STATUS;
        }

        final boolean autoResolveConflicts = TFSCommonUIClientPlugin.getDefault().getPreferenceStore().getBoolean(
            UIPreferenceConstants.AUTO_RESOLVE_CONFLICTS);

        return unshelve(
            unshelveDialog.getSelectedShelveset(),
            unshelveDialog.getCheckedItemSpecs(),
            (unshelveDialog.isPreserveShelveset() == false),
            unshelveDialog.isRestoreData(),
            autoResolveConflicts);
    }
}
