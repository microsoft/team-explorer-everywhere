// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.vc.branch;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.framework.image.ImageHelper;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.BranchObject;

public class BranchObjectModelLabelProvider extends LabelProvider {
    @Override
    public String getText(final Object element) {
        if (element instanceof BranchObject) {
            final BranchObject b = (BranchObject) element;
            final String serverPath = b.getProperties().getRootItem().getItem();
            return ServerPath.getFileName(serverPath);
        }
        return super.getText(element);
    }

    @Override
    public Image getImage(final Object element) {
        if (element instanceof BranchObject) {
            return new ImageHelper(TFSCommonUIClientPlugin.PLUGIN_ID).getImage("images/vc/folder_branch.gif"); //$NON-NLS-1$
        }
        return super.getImage(element);
    }
}