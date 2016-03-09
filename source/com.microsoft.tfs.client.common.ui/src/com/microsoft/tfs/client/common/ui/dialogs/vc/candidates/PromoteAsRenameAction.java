// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.dialogs.vc.candidates;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;

import org.eclipse.jface.viewers.IStructuredSelection;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.controls.vc.changes.ChangeItem;
import com.microsoft.tfs.client.common.ui.dialogs.vc.PromoteCandidateChangesDialog;
import com.microsoft.tfs.client.common.ui.framework.helper.SelectionUtils;
import com.microsoft.tfs.core.clients.versioncontrol.GetOptions;
import com.microsoft.tfs.core.clients.versioncontrol.PendChangesOptions;
import com.microsoft.tfs.core.clients.versioncontrol.exceptions.VersionControlException;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ChangeType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.LockLevel;
import com.microsoft.tfs.util.FileHelpers;

public class PromoteAsRenameAction extends CandidateAction {
    public PromoteAsRenameAction(final PromoteCandidateChangesDialog dialog) {
        super(dialog);
        setText(Messages.getString("PromoteAsRenameAction.PromoteAsRenameActionText")); //$NON-NLS-1$
    }

    @Override
    public void doRun() {
        final ChangeItem[] changes = (ChangeItem[]) SelectionUtils.selectionToArray(getSelection(), ChangeItem.class);

        ChangeItem add = null;
        ChangeItem delete = null;

        if (changes[0].getChangeType().contains(ChangeType.ADD)) {
            add = changes[0];
        } else if (changes[1].getChangeType().contains(ChangeType.ADD)) {
            add = changes[1];
        }

        if (changes[0].getChangeType().contains(ChangeType.DELETE)) {
            delete = changes[0];
        } else if (changes[1].getChangeType().contains(ChangeType.DELETE)) {
            delete = changes[1];
        }

        if (add != null && delete != null) {
            final File addFile = new File(add.getPendingChange().getLocalItem());
            final File deleteFile = new File(delete.getPendingChange().getLocalItem());

            try {
                FileHelpers.rename(addFile, deleteFile);
            } catch (final IOException e) {
                if (deleteFile.exists()) {
                    throw new VersionControlException(
                        MessageFormat.format(
                            Messages.getString("PromoteAsRenameAction.ItemAlreadyExistsFormat"), //$NON-NLS-1$
                            deleteFile));
                } else if (!addFile.exists()) {
                    throw new VersionControlException(
                        MessageFormat.format(Messages.getString("PromoteAsRenameAction.ItemNotFoundFormat"), addFile)); //$NON-NLS-1$
                }

                throw new VersionControlException(e);
            }

            try {
                boolean success = false;
                try {
                    success = repository.getWorkspace().pendRename(
                        deleteFile.getAbsolutePath(),
                        addFile.getAbsolutePath(),
                        LockLevel.UNCHANGED,
                        GetOptions.NONE,
                        false,
                        PendChangesOptions.NONE) == 1;
                } finally {
                    if (!success) {
                        try {
                            FileHelpers.rename(deleteFile, addFile);
                        } catch (final IOException e) {
                            throw new VersionControlException(e);
                        }
                    }
                }
            } finally {
                dialog.refreshCandidateTable(changes);
            }
        }
    }

    @Override
    protected boolean computeEnablement(final IStructuredSelection selection) {
        final ChangeItem[] changes = (ChangeItem[]) SelectionUtils.selectionToArray(getSelection(), ChangeItem.class);

        if (changes.length != 2) {
            return false;
        }

        // Must be one add and one delete
        return (changes[0].getChangeType().contains(ChangeType.ADD)
            && changes[1].getChangeType().contains(ChangeType.DELETE))
            || (changes[0].getChangeType().contains(ChangeType.DELETE)
                && changes[1].getChangeType().contains(ChangeType.ADD));
    }

    public boolean isVisible(final IStructuredSelection selection) {
        final ChangeItem[] changes = (ChangeItem[]) SelectionUtils.selectionToArray(getSelection(), ChangeItem.class);

        boolean add = false;
        boolean delete = false;

        // Show for at least one add and at least one one delete
        for (final ChangeItem change : changes) {
            if (change.getChangeType().contains(ChangeType.ADD)) {
                add = true;
            }

            if (change.getChangeType().contains(ChangeType.DELETE)) {
                delete = true;
            }

            if (add && delete) {
                return true;
            }
        }

        return false;
    }
}
