// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teamexplorer.internal.pendingchanges;

import java.text.MessageFormat;

import org.eclipse.swt.graphics.Image;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.framework.viewer.FolderFileLabelProvider;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;
import com.microsoft.tfs.util.Check;

public class PendingChangesTreeLabelProvider extends FolderFileLabelProvider {
    @Override
    public String getText(final Object element) {
        Check.isTrue(element instanceof PendingChangesTreeNode, "element instanceof PendingChangesTreeNode"); //$NON-NLS-1$
        final PendingChangesTreeNode node = (PendingChangesTreeNode) element;
        final PendingChange change = node.getPendingChange();

        if (change == null) {
            return node.getSubpath();
        }

        final String changeType = change.getChangeType().toUIString(true, change.getPropertyValues());
        return MessageFormat.format(
            Messages.getString("PendingChangesTreeLabelProvider.PendingChangeTreeNodeFormat"), //$NON-NLS-1$
            node.getSubpath(),
            changeType);
    }

    @Override
    public Image getImage(final Object element) {
        Check.isTrue(element instanceof PendingChangesTreeNode, "element instanceof PendingChangesTreeNode"); //$NON-NLS-1$
        final PendingChangesTreeNode node = (PendingChangesTreeNode) element;
        final PendingChange change = node.getPendingChange();

        if (change == null || change.getItemType() == ItemType.FOLDER) {
            return getImageForFolder();
        }

        return getImageForFile(change.getLocalItem());
    }
}
