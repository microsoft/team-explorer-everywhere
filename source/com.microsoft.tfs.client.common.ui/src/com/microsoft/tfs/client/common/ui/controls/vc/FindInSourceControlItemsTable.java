// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.vc;

import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.framework.helper.TableViewerSorter;
import com.microsoft.tfs.client.common.ui.framework.helper.TableViewerSorter.SortDirection;
import com.microsoft.tfs.client.common.ui.framework.image.ImageHelper;
import com.microsoft.tfs.client.common.ui.framework.table.TableColumnData;
import com.microsoft.tfs.client.common.ui.framework.table.TableControl;
import com.microsoft.tfs.client.common.ui.framework.table.TableViewerUtils;
import com.microsoft.tfs.client.common.ui.framework.viewer.FolderFileLabelProvider;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Item;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemType;

public class FindInSourceControlItemsTable extends TableControl {
    private static final String NAME_COLUMN_ID = "name"; //$NON-NLS-1$
    private static final String FOLDER_COLUMN_ID = "folder"; //$NON-NLS-1$

    public FindInSourceControlItemsTable(final Composite parent, final int style) {
        super(parent, style, Item.class, null);

        final TableColumnData[] columnData = new TableColumnData[] {
            new TableColumnData(
                Messages.getString("FindInSourceControlItemsTable.ColumnHeaderName"), //$NON-NLS-1$
                100,
                0.2F,
                NAME_COLUMN_ID),
            new TableColumnData(
                Messages.getString("FindInSourceControlItemsTable.ColumnHeaderFolder"), //$NON-NLS-1$
                400,
                0.8F,
                FOLDER_COLUMN_ID),
        };

        setupTable(true, true, columnData);

        setUseDefaultContentProvider();
        getViewer().setSorter(new TableViewerSorter(getViewer(), null, 1, SortDirection.ASCENDING));
        getViewer().setLabelProvider(new FindInSourceControlItemLabelProvider());
    }

    public void setItems(final Item[] items) {
        setElements(items);
    }

    public Item[] getItems() {
        return (Item[]) getElements();
    }

    public Item[] getSelectedItems() {
        return (Item[]) getSelectedElements();
    }

    private class FindInSourceControlItemLabelProvider extends FolderFileLabelProvider implements ITableLabelProvider {
        private final ImageHelper imageHelper = new ImageHelper(TFSCommonUIClientPlugin.PLUGIN_ID);

        @Override
        public Image getColumnImage(final Object element, final int columnIndex) {
            if (columnIndex != 0) {
                return null;
            }

            final Item item = (Item) element;

            if (ItemType.FOLDER == item.getItemType() && item.isBranch()) {
                return imageHelper.getImage("images/vc/folder_branch.gif"); //$NON-NLS-1$
            }
            if (ServerPath.equals(ServerPath.ROOT, item.getServerItem())) {
                return imageHelper.getImage("images/common/team_foundation_server.gif"); //$NON-NLS-1$
            } else if (ServerPath.equals(item.getServerItem(), ServerPath.getTeamProject(item.getServerItem()))) {
                return imageHelper.getImage("images/common/team_project.gif"); //$NON-NLS-1$
            } else if (ItemType.FOLDER == item.getItemType()) {
                return getImageForFolder();
            }

            return getImageForFile(ServerPath.getFileName(item.getServerItem()));
        }

        @Override
        public String getColumnText(final Object element, final int columnIndex) {
            final String columnId = TableViewerUtils.columnIndexToColumnProperty(columnIndex, getViewer());

            final Item item = (Item) element;

            if (NAME_COLUMN_ID.equals(columnId)) {
                return ServerPath.getFileName(item.getServerItem());
            } else if (FOLDER_COLUMN_ID.equals(columnId)) {
                return ServerPath.getParent(item.getServerItem());
            } else {
                return ""; //$NON-NLS-1$
            }
        }

        /**
         * {@link FolderFileLabelProvider} overrides this {@link LabelProvider}
         * method to clean up the {@link ImageHelper} that it manages.
         * Subclasses must call super.dispose() if they also override this
         * method.
         */
        @Override
        public void dispose() {
            imageHelper.dispose();
        }
    }
}
