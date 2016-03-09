// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.compare;

import java.text.MessageFormat;

import org.eclipse.compare.CompareUI;
import org.eclipse.compare.ITypedElement;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.graphics.Image;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.framework.compare.CompareUtils;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlClient;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ExtendedItem;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Item;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemType;
import com.microsoft.tfs.core.util.CodePageMapping;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.LocaleUtil;

public class TFSExtendedItemNode extends TFSItemCompareNode {
    private final ExtendedItem extendedItem;
    private final VersionControlClient vcClient;

    private Item downloadItem;

    public TFSExtendedItemNode(final ExtendedItem item, final VersionControlClient vcClient) {
        super(vcClient);

        Check.notNull(item, "item"); //$NON-NLS-1$
        Check.notNull(vcClient, "vcClient"); //$NON-NLS-1$

        extendedItem = item;
        this.vcClient = vcClient;
    }

    public ExtendedItem getItem() {
        return extendedItem;
    }

    @Override
    public String toString() {
        final String messageFormat = Messages.getString("TFSExtendedItemNode.ToStringFormat"); //$NON-NLS-1$
        return MessageFormat.format(
            messageFormat,
            extendedItem.getSourceServerItem(),
            Integer.toString(extendedItem.getLatestVersion()));
    }

    /*
     * START: ITypedElement
     */

    @Override
    public Image getImage() {
        return CompareUI.getImage(getType());
    }

    @Override
    public String getName() {
        return ServerPath.getFileName(extendedItem.getSourceServerItem());
    }

    @Override
    public String getType() {
        if (extendedItem.getItemType() == ItemType.FOLDER) {
            return ITypedElement.FOLDER_TYPE;
        }

        return CompareUtils.computeTypeFromFilename(getName());
    }

    /*
     * END: ITypedElement
     */

    /*
     * START: IEncodedStreamContentAccessor
     */

    @Override
    public String getCharset() throws CoreException {
        final int codePage = extendedItem.getEncoding().getCodePage();
        return CodePageMapping.getEncoding(codePage, false, false);
    }

    /*
     * END: IEncodedStreamContentAccessor
     */

    /*
     * START: ILabeledCompareElement
     */

    @Override
    public String getLabel() {
        final String messageFormat = Messages.getString("TFSExtendedItemNode.LabelFormat"); //$NON-NLS-1$
        return MessageFormat.format(
            messageFormat,
            extendedItem.getSourceServerItem(),
            Integer.toString(extendedItem.getLatestVersion()));
    }

    @Override
    public String getLabelNOLOC() {
        final String messageFormat = Messages.getString("TFSExtendedItemNode.LabelFormat", LocaleUtil.ROOT); //$NON-NLS-1$
        return MessageFormat.format(
            messageFormat,
            extendedItem.getSourceServerItem(),
            Integer.toString(extendedItem.getLatestVersion()));
    }

    /*
     * END: ILabeledCompareElement
     */

    @Override
    protected Item getDownloadItem() {
        if (downloadItem == null) {
            downloadItem = vcClient.getItem(extendedItem.getItemID(), extendedItem.getLatestVersion(), true);
        }

        return downloadItem;
    }
}
