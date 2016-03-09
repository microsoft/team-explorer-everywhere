// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.vc.properties.branches;

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.IFontProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.framework.WindowSystem;
import com.microsoft.tfs.core.clients.versioncontrol.BranchHistoryTreeItem;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ChangeType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Item;
import com.microsoft.tfs.util.Platform;

public class BranchesPropertiesLabelProvider extends LabelProvider implements ITableLabelProvider, IFontProvider {
    private static final int MAC_PADDING_SPACES = 7;
    private static final int GTK_PADDING_SPACES = 4;

    @Override
    public Image getColumnImage(final Object element, final int columnIndex) {
        return null;
    }

    @Override
    public String getColumnText(final Object element, final int columnIndex) {
        final BranchHistoryTreeItem historyItem = (BranchHistoryTreeItem) element;
        switch (columnIndex) {
            case 0: // Server Item
                final Item item = historyItem.getItem();

                final String serverPath = (item != null) ? item.getServerItem()
                    : Messages.getString("BranchesPropertiesLabelProvider.NoPermissionToReadBranch"); //$NON-NLS-1$

                /*
                 * Mac OS X doesn't leave enough room for the expansion button,
                 * so we have to calculate manual padding.
                 */
                if (Platform.isCurrentPlatform(Platform.MAC_OS_X)) {
                    return getMacPadding(historyItem) + serverPath;
                } else if (WindowSystem.isCurrentWindowSystem(WindowSystem.GTK)) {
                    /*
                     * GTK supplies enough padding for the first level items (to
                     * leave room for the expansion button), but gives no
                     * further indent for nested levels.
                     */
                    return getGTKPadding(historyItem) + serverPath;
                } else {
                    return serverPath;
                }

            case 1:
                return ChangeType.fromIntFlags(0, historyItem.getBranchToChangeTypeEx()).toUIString(
                    true,
                    historyItem.getItem());

            case 2: // change type
                if (historyItem.getParent() == null) {
                    return ""; //$NON-NLS-1$
                }
                return "" + historyItem.getFromItemChangesetID(); //$NON-NLS-1$

        }
        return ""; //$NON-NLS-1$
    }

    private String getMacPadding(final BranchHistoryTreeItem item) {
        final int paddingSpaces = item.getLevel() * MAC_PADDING_SPACES;
        final StringBuffer padding = new StringBuffer(paddingSpaces);
        for (int i = 0; i < paddingSpaces; i++) {
            padding.append(' ');
        }
        return padding.toString();
    }

    private String getGTKPadding(final BranchHistoryTreeItem item) {
        final int paddingSpaces = (item.getLevel()) * GTK_PADDING_SPACES;
        final StringBuffer padding = new StringBuffer(paddingSpaces);
        for (int i = 0; i < paddingSpaces; i++) {
            padding.append(' ');
        }
        return padding.toString();
    }

    @Override
    public Font getFont(final Object element) {
        // Make tree item bold if it is the one that we requested branch
        // history on.
        if (((BranchHistoryTreeItem) element).isRequested()) {
            return JFaceResources.getFontRegistry().getBold(JFaceResources.DEFAULT_FONT);
        }
        return null;
    }

}
