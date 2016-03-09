// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.dialogs.vc.candidates;

import org.eclipse.jface.viewers.IStructuredSelection;

import com.microsoft.tfs.client.common.ui.controls.vc.changes.ChangeItem;
import com.microsoft.tfs.client.common.ui.dialogs.vc.PromoteCandidateChangesDialog;
import com.microsoft.tfs.client.common.ui.framework.helper.SelectionUtils;
import com.microsoft.tfs.core.clients.versioncontrol.path.LocalPath;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ChangeType;

/**
 * Base action for candidate dialog ignore context menu actions in this package.
 */
abstract class IgnoreAction extends CandidateAction {
    public IgnoreAction(final PromoteCandidateChangesDialog dialog) {
        super(dialog);
    }

    public final boolean isVisible(final IStructuredSelection selection) {
        final ChangeItem[] changes = (ChangeItem[]) SelectionUtils.selectionToArray(getSelection(), ChangeItem.class);

        // Show if any is an add
        for (final ChangeItem change : changes) {
            if (change.getChangeType().contains(ChangeType.ADD)) {
                return true;
            }
        }

        return false;
    }

    protected static String getExtension(final ChangeItem change) {
        final String path = change.getPendingChange().getLocalItem();
        return LocalPath.getFileExtension(path);
    }

    protected static String getFileName(final ChangeItem change) {
        final String path = change.getPendingChange().getLocalItem();
        return LocalPath.getFileName(path);
    }

    protected static String getFolderName(final ChangeItem change) {
        final String path = change.getPendingChange().getLocalItem();
        return LocalPath.getLastComponent(LocalPath.getParent(path));
    }
}