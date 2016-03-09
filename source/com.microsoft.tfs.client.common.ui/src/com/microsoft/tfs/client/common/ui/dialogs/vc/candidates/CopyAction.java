// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.dialogs.vc.candidates;

import org.eclipse.jface.viewers.IStructuredSelection;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.TFSCommonUIImages;
import com.microsoft.tfs.client.common.ui.controls.vc.changes.ChangeItem;
import com.microsoft.tfs.client.common.ui.dialogs.vc.PromoteCandidateChangesDialog;
import com.microsoft.tfs.client.common.ui.framework.helper.SelectionUtils;
import com.microsoft.tfs.client.common.ui.framework.helper.UIHelpers;
import com.microsoft.tfs.util.NewlineUtils;

public class CopyAction extends CandidateAction {
    public CopyAction(final PromoteCandidateChangesDialog dialog) {
        super(dialog);
        setText(Messages.getString("CopyAction.CopyActionText")); //$NON-NLS-1$
        setImageDescriptor(TFSCommonUIImages.getImageDescriptor(TFSCommonUIImages.IMG_COPY));
    }

    @Override
    public void doRun() {
        final StringBuilder sb = new StringBuilder();

        final ChangeItem[] changes = (ChangeItem[]) SelectionUtils.selectionToArray(getSelection(), ChangeItem.class);

        for (final ChangeItem change : changes) {
            if (sb.length() > 0) {
                sb.append(NewlineUtils.PLATFORM_NEWLINE);
            }

            sb.append(change.getName());
            sb.append('\t');
            sb.append(change.getChangeType().toUIString(true));
            sb.append('\t');
            sb.append(change.getFolder());
        }

        UIHelpers.copyToClipboard(sb.toString());
    }

    @Override
    protected boolean computeEnablement(final IStructuredSelection selection) {
        return selection.size() > 0;
    }
}
