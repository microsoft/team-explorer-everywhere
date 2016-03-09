// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.vc.serveritem;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.framework.viewer.FolderFileLabelProvider;

public class ServerItemLabelProvider extends FolderFileLabelProvider {
    private Image serverImage;
    private Image teamProjectImage;
    private Image branchImage;
    private Image gitBranchImage;
    private Image gitRepositoryImage;

    private ServerItemSource serverItemSource;

    public void setServerItemSource(final ServerItemSource serverItemSource) {
        this.serverItemSource = serverItemSource;
    }

    @Override
    public String getText(final Object element) {
        final TypedServerItem node = (TypedServerItem) element;

        if (node.getType() == ServerItemType.ROOT && serverItemSource != null) {
            return serverItemSource.getServerName();
        }

        return node.getName();
    }

    @Override
    public Image getImage(final Object element) {
        final TypedServerItem node = (TypedServerItem) element;

        if (node.getType() == ServerItemType.ROOT) {
            if (serverImage == null) {
                final ImageDescriptor imageDescriptor = AbstractUIPlugin.imageDescriptorFromPlugin(
                    TFSCommonUIClientPlugin.PLUGIN_ID,
                    "icons/TeamFoundationServer.gif"); //$NON-NLS-1$
                serverImage = getImageHelper().getImage(imageDescriptor);
            }
            return serverImage;
        }

        if (node.getType() == ServerItemType.TEAM_PROJECT) {
            if (teamProjectImage == null) {
                final ImageDescriptor imageDescriptor = AbstractUIPlugin.imageDescriptorFromPlugin(
                    TFSCommonUIClientPlugin.PLUGIN_ID,
                    "icons/TeamProject.gif"); //$NON-NLS-1$
                teamProjectImage = getImageHelper().getImage(imageDescriptor);
            }
            return teamProjectImage;
        }

        if (node.getType() == ServerItemType.FOLDER && node.isBranch()) {
            if (branchImage == null) {
                final ImageDescriptor imageDescriptor = AbstractUIPlugin.imageDescriptorFromPlugin(
                    TFSCommonUIClientPlugin.PLUGIN_ID,
                    "images/vc/folder_branch.gif"); //$NON-NLS-1$
                branchImage = getImageHelper().getImage(imageDescriptor);
            }
            return branchImage;
        }

        if (node.getType() == ServerItemType.GIT_REPOSITORY) {
            if (gitRepositoryImage == null) {
                final ImageDescriptor imageDescriptor = AbstractUIPlugin.imageDescriptorFromPlugin(
                    TFSCommonUIClientPlugin.PLUGIN_ID,
                    "images/common/git_repo.png"); //$NON-NLS-1$
                gitRepositoryImage = getImageHelper().getImage(imageDescriptor);
            }
            return gitRepositoryImage;
        }

        if (node.getType() == ServerItemType.GIT_BRANCH) {
            if (gitBranchImage == null) {
                final ImageDescriptor imageDescriptor = AbstractUIPlugin.imageDescriptorFromPlugin(
                    TFSCommonUIClientPlugin.PLUGIN_ID,
                    "images/common/git_branch.png"); //$NON-NLS-1$
                gitBranchImage = getImageHelper().getImage(imageDescriptor);
            }
            return gitBranchImage;
        }

        if (node.getType() == ServerItemType.FOLDER) {
            return getImageForFolder();
        } else {
            return getImageForFile(node.getName());
        }
    }
}
