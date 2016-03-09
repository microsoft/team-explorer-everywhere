// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.vc.tfsitem;

import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.framework.image.ImageHelper;
import com.microsoft.tfs.client.common.ui.framework.viewer.FolderFileLabelProvider;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ExtendedItem;

/**
 * This is a base class for label providers that display TFSItems, like those
 * used in the FolderControl and the FileControl. The FolderControl and
 * FileControl both use subclasses of TFSItemLabelProvider that provide
 * functionality specific to those controls.
 *
 * Note: although this class implements ITableLabelProvider, subclasses of it
 * could be used equally well for tree and table label providers. The
 * ITableLabelProvider implementation is a convenience implementation for
 * subclasses.
 */
public class TFSItemLabelProvider extends FolderFileLabelProvider implements IColorProvider, ITableLabelProvider {
    private final ImageHelper imageHelper = new ImageHelper(TFSCommonUIClientPlugin.PLUGIN_ID);

    private TFSRepository getRepository() {
        return TFSCommonUIClientPlugin.getDefault().getProductPlugin().getRepositoryManager().getDefaultRepository();
    }

    @Override
    public Image getImage(final Object element) {
        if (element instanceof TFSItem) {
            return getImageForTFSItem(getRepository(), (TFSItem) element);
        }
        return super.getImage(element);
    }

    @Override
    public String getText(final Object obj) {
        if (obj instanceof TFSItem) {
            return ((TFSItem) obj).getName();
        }
        return super.getText(obj);
    }

    @Override
    public Image getColumnImage(final Object element, final int columnIndex) {
        if (element instanceof TFSItem) {
            return getColumnImageForTFSItem(getRepository(), (TFSItem) element, columnIndex);
        }
        return null;
    }

    @Override
    public String getColumnText(final Object element, final int columnIndex) {
        if (element instanceof TFSItem) {
            return getColumnTextForTFSItem(getRepository(), (TFSItem) element, columnIndex);
        }
        return null;
    }

    @Override
    public Color getForeground(final Object element) {
        if (element instanceof TFSItem) {
            return getForegroundColorForTFSItem(getRepository(), (TFSItem) element);
        }
        return null;
    }

    @Override
    public Color getBackground(final Object element) {
        if (element instanceof TFSItem) {
            return getBackgroundColorForTFSItem(getRepository(), (TFSItem) element);
        }
        return null;
    }

    protected Image getColumnImageForTFSItem(
        final TFSRepository repository,
        final TFSItem item,
        final int columnIndex) {
        if (columnIndex == 0) {
            return getImageForTFSItem(repository, item);
        }
        return null;
    }

    protected String getColumnTextForTFSItem(
        final TFSRepository repository,
        final TFSItem item,
        final int columnIndex) {
        return getText(item);
    }

    /**
     * Return the foreground color to use for the given TFSItem. The default
     * implementation returns null, using the default color. Overrides can call
     * getSystemColor to return a specific color.
     *
     * @param item
     *        input TFSItem
     * @return a foreground color for the TFSItem, or null
     */
    protected Color getForegroundColorForTFSItem(final TFSRepository repository, final TFSItem item) {
        return null;
    }

    /**
     * Return the background color to use for the given TFSItem. The default
     * implementation returns null, using the default color. Overrides can call
     * getSystemColor to return a specific color.
     *
     * @param item
     *        input TFSItem
     * @return a background color for the TFSItem, or null
     */
    protected Color getBackgroundColorForTFSItem(final TFSRepository repository, final TFSItem item) {
        return null;
    }

    /**
     * Returns a Color object given one of the IDs defined on the SWT class, for
     * example, SWT.COLOR_BLACK. The returned Color should not be disposed of by
     * the caller.
     *
     * @see Display.getSystemColor()
     * @param colorId
     *        SWT color ID
     * @return a Color to use
     */
    protected Color getSystemColor(final int colorId) {
        return Display.getCurrent().getSystemColor(colorId);
    }

    /**
     * Return an icon suitable for displaying next to the given object. If the
     * object is not a TFSItem, returns null.
     *
     * @param obj
     *        input object
     * @return a folder icon, file icon, or null
     */
    protected Image getImageForTFSItem(final TFSRepository repository, final TFSItem item) {
        final String serverPath = ServerPath.canonicalize(item.getFullPath());

        final ExtendedItem extendedItem = item.getExtendedItem();

        if (item instanceof TFSFolder) {
            /* Handle branches */
            if ((extendedItem != null && extendedItem.isBranch())) {
                if (item.isDeleted()) {
                    return imageHelper.getImage("images/vc/folder_branch_deleted.gif"); //$NON-NLS-1$
                }

                return imageHelper.getImage("images/vc/folder_branch.gif"); //$NON-NLS-1$
            }

            if (ServerPath.equals(ServerPath.ROOT, item.getFullPath())) {
                return imageHelper.getImage("images/common/team_foundation_server.gif"); //$NON-NLS-1$
            } else if (ServerPath.equals(serverPath, ServerPath.getTeamProject(serverPath))) {
                return imageHelper.getImage("images/common/team_project.gif"); //$NON-NLS-1$
            } else if (item.isDeleted()) {
                return imageHelper.getImage("images/vc/folder_deleted.gif"); //$NON-NLS-1$
            }

            return PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FOLDER);
        } else if (item instanceof TFSFile) {
            return getImageForFile(ServerPath.getFileName(serverPath));
        } else {
            return null;
        }
    }

    @Override
    public void dispose() {
        imageHelper.dispose();
        super.dispose();
    }
}
