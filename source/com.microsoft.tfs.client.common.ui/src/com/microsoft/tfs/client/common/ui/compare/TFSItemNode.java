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
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Item;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemType;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.ChangesetVersionSpec;
import com.microsoft.tfs.core.util.CodePageMapping;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.LocaleUtil;

public class TFSItemNode extends TFSItemCompareNode {
    private final Item item;
    private final VersionControlClient vcClient;

    private Item downloadItem;

    public TFSItemNode(final Item item, final VersionControlClient vcClient) {
        super(vcClient);

        Check.notNull(item, "item"); //$NON-NLS-1$
        Check.notNull(vcClient, "vcClient"); //$NON-NLS-1$

        this.item = item;
        this.vcClient = vcClient;
    }

    public Item getItem() {
        return item;
    }

    @Override
    public String toString() {
        return item.getServerItem() + ";C" + item.getChangeSetID(); //$NON-NLS-1$
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
        return ServerPath.getFileName(item.getServerItem());
    }

    @Override
    public String getType() {
        if (item.getItemType() == ItemType.FOLDER) {
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
        final int codePage = item.getEncoding().getCodePage();
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
        final String messageFormat = Messages.getString("TFSItemNode.CompareElementLabelFormat"); //$NON-NLS-1$
        return MessageFormat.format(messageFormat, item.getServerItem(), Integer.toString(item.getChangeSetID()));
    }

    @Override
    public String getLabelNOLOC() {
        final String messageFormat = Messages.getString("TFSItemNode.CompareElementLabelFormat", LocaleUtil.ROOT); //$NON-NLS-1$
        return MessageFormat.format(messageFormat, item.getServerItem(), Integer.toString(item.getChangeSetID()));
    }

    /*
     * END: ILabeledCompareElement
     */

    /*
     * BEGIN: TFSCompareNode
     */

    /*
     * The AItem that we were instantiated with may not contain a download URL.
     * If that's the case, we need to requery the server.
     */
    @Override
    protected Item getDownloadItem() {
        if (downloadItem == null) {
            if (item.getDownloadURL() != null) {
                downloadItem = item;
            } else {
                downloadItem = vcClient.getItem(
                    item.getServerItem(),
                    new ChangesetVersionSpec(item.getChangeSetID()),
                    item.getDeletionID(),
                    true);
                // downloadItem = vcClient.getItem(item.getItemID(),
                // item.getChangeSetID(), true);
            }
        }

        return downloadItem;
    }

    /*
     * END: TFSCompareNode
     */
}
